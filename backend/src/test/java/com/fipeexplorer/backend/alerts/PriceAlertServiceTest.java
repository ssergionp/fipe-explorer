package com.fipeexplorer.backend.alerts;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.FuelType;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.domain.WatchedVehicle;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import com.fipeexplorer.backend.repository.WatchedVehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock
    private WatchedVehicleRepository watchedVehicleRepository;
    @Mock
    private VehicleModelRepository vehicleModelRepository;
    @Mock
    private PriceEntryRepository priceEntryRepository;
    @Mock
    private PriceAlertMailService mailService;

    private final Brand brand = new Brand("1", "Marca Teste");
    private final FuelType gasolina = new FuelType("G", "Gasolina");
    private final User user = new User("watcher@example.com", "hash");

    private PriceAlertService newService() {
        return new PriceAlertService(watchedVehicleRepository, vehicleModelRepository, priceEntryRepository, mailService);
    }

    private VehicleModel vehicleModel(String fipeCode) {
        return new VehicleModel(brand, "M1", "Modelo Teste", "CAR", fipeCode);
    }

    private WatchedVehicle watch(String fipeCode, String thresholdPercent) {
        return new WatchedVehicle(user, fipeCode, new BigDecimal(thresholdPercent));
    }

    private PriceEntry entry(VehicleModel model, String yearCode, String price, String isoDate) {
        return new PriceEntry(model, gasolina, yearCode, yearCode + " Gasolina", new BigDecimal(price),
                "mês de teste", LocalDate.parse(isoDate));
    }

    @Test
    void priceIncreaseAboveThresholdTriggersEmail() {
        VehicleModel model = vehicleModel("111111-1");
        WatchedVehicle watched = watch("111111-1", "0.05");
        List<PriceEntry> entries = List.of(
                entry(model, "2020-1", "10000.00", "2026-07-01"),
                entry(model, "2020-1", "11000.00", "2026-08-01")); // +10%

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(watched));
        when(vehicleModelRepository.findByFipePriceCode("111111-1")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService).sendPriceAlert(eq(user), eq(model), any());
    }

    @Test
    void priceDropAboveThresholdAlsoTriggersEmail() {
        VehicleModel model = vehicleModel("222222-2");
        WatchedVehicle watched = watch("222222-2", "0.05");
        List<PriceEntry> entries = List.of(
                entry(model, "2020-1", "10000.00", "2026-07-01"),
                entry(model, "2020-1", "9000.00", "2026-08-01")); // -10%

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(watched));
        when(vehicleModelRepository.findByFipePriceCode("222222-2")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService).sendPriceAlert(eq(user), eq(model), any());
    }

    @Test
    void priceChangeBelowThresholdDoesNotTriggerEmail() {
        VehicleModel model = vehicleModel("333333-3");
        WatchedVehicle watched = watch("333333-3", "0.05");
        List<PriceEntry> entries = List.of(
                entry(model, "2020-1", "10000.00", "2026-07-01"),
                entry(model, "2020-1", "10200.00", "2026-08-01")); // +2%, abaixo dos 5%

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(watched));
        when(vehicleModelRepository.findByFipePriceCode("333333-3")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService, never()).sendPriceAlert(any(), any(), any());
    }

    @Test
    void customThresholdIsRespected() {
        VehicleModel model = vehicleModel("444444-4");
        WatchedVehicle watched = watch("444444-4", "0.15"); // 15% - mais tolerante que o default
        List<PriceEntry> entries = List.of(
                entry(model, "2020-1", "10000.00", "2026-07-01"),
                entry(model, "2020-1", "11000.00", "2026-08-01")); // +10%, abaixo do threshold custom

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(watched));
        when(vehicleModelRepository.findByFipePriceCode("444444-4")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService, never()).sendPriceAlert(any(), any(), any());
    }

    @Test
    void lineWithNoPriorPriceIsSkippedWithoutFalseAlert() {
        VehicleModel model = vehicleModel("555555-5");
        WatchedVehicle watched = watch("555555-5", "0.05");
        // só uma linha pra essa combinação (ano/combustível) - nada pra comparar ainda.
        List<PriceEntry> entries = List.of(entry(model, "2020-1", "10000.00", "2026-08-01"));

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(watched));
        when(vehicleModelRepository.findByFipePriceCode("555555-5")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService, never()).sendPriceAlert(any(), any(), any());
    }

    @Test
    void unresolvableFipeCodeDoesNotCrashCheckOfOtherWatches() {
        WatchedVehicle unresolvable = watch("999999-9", "0.05");

        VehicleModel model = vehicleModel("666666-6");
        WatchedVehicle resolvable = watch("666666-6", "0.05");
        List<PriceEntry> entries = List.of(
                entry(model, "2020-1", "10000.00", "2026-07-01"),
                entry(model, "2020-1", "20000.00", "2026-08-01")); // +100%

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(unresolvable, resolvable));
        when(vehicleModelRepository.findByFipePriceCode("999999-9")).thenReturn(Optional.empty());
        when(vehicleModelRepository.findByFipePriceCode("666666-6")).thenReturn(Optional.of(model));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any())).thenReturn(entries);

        newService().checkAllWatchedVehicles();

        verify(mailService).sendPriceAlert(eq(user), eq(model), any());
    }

    @Test
    void multipleWatchedVehiclesAreCheckedIndependently() {
        VehicleModel triggers = vehicleModel("777777-7");
        VehicleModel doesNotTrigger = vehicleModel("888888-8");

        when(watchedVehicleRepository.findAll()).thenReturn(List.of(
                watch("777777-7", "0.05"), watch("888888-8", "0.05")));
        when(vehicleModelRepository.findByFipePriceCode("777777-7")).thenReturn(Optional.of(triggers));
        when(vehicleModelRepository.findByFipePriceCode("888888-8")).thenReturn(Optional.of(doesNotTrigger));
        when(priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(any()))
                .thenReturn(List.of(
                        entry(triggers, "2020-1", "10000.00", "2026-07-01"),
                        entry(triggers, "2020-1", "12000.00", "2026-08-01"))) // +20%
                .thenReturn(List.of(
                        entry(doesNotTrigger, "2020-1", "10000.00", "2026-07-01"),
                        entry(doesNotTrigger, "2020-1", "10010.00", "2026-08-01"))); // +0.1%

        newService().checkAllWatchedVehicles();

        verify(mailService).sendPriceAlert(eq(user), eq(triggers), any());
        verify(mailService, never()).sendPriceAlert(eq(user), eq(doesNotTrigger), any());
    }
}
