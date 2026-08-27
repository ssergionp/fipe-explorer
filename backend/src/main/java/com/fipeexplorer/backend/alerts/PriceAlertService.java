package com.fipeexplorer.backend.alerts;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.domain.WatchedVehicle;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import com.fipeexplorer.backend.repository.WatchedVehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Roda depois de todo import que trouxe dado novo (ver ImportOrchestrator) - startup, cron e
 * trigger manual disparam o mesmo método. Pra cada veículo observado, compara o preço mais
 * recente de cada combinação (ano, combustível) daquele modelo contra o valor anterior disponível
 * da MESMA combinação (o dado mais recente que já existia antes deste import, não
 * necessariamente o mês civil anterior). Combinação sem preço anterior (ano/combustível novo) é
 * ignorada - não gera alerta falso. Considera queda e alta (variação absoluta): quem decide o que
 * fazer com a informação é a pessoa observando.
 */
@Service
public class PriceAlertService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertService.class);

    private final WatchedVehicleRepository watchedVehicleRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final PriceEntryRepository priceEntryRepository;
    private final PriceAlertMailService mailService;

    public PriceAlertService(WatchedVehicleRepository watchedVehicleRepository,
                              VehicleModelRepository vehicleModelRepository,
                              PriceEntryRepository priceEntryRepository,
                              PriceAlertMailService mailService) {
        this.watchedVehicleRepository = watchedVehicleRepository;
        this.vehicleModelRepository = vehicleModelRepository;
        this.priceEntryRepository = priceEntryRepository;
        this.mailService = mailService;
    }

    public record PriceChange(
            String yearCode,
            String yearValue,
            String fuel,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            BigDecimal changePercent
    ) {
    }

    public void checkAllWatchedVehicles() {
        List<WatchedVehicle> watches = watchedVehicleRepository.findAll();
        log.info("Checando alertas de preço pra {} veículo(s) observado(s).", watches.size());
        for (WatchedVehicle watch : watches) {
            try {
                checkWatchedVehicle(watch);
            } catch (Exception e) {
                // um veículo com problema não pode travar a checagem dos outros observados.
                log.error("Falha ao checar alerta pro fipe_code {}: {}", watch.getFipeCode(), e.getMessage(), e);
            }
        }
    }

    private void checkWatchedVehicle(WatchedVehicle watch) {
        VehicleModel vehicleModel = vehicleModelRepository.findByFipePriceCode(watch.getFipeCode()).orElse(null);
        if (vehicleModel == null) {
            log.warn("fipe_code {} observado não corresponde a nenhum veículo - pulando.", watch.getFipeCode());
            return;
        }

        List<PriceChange> changes = significantChanges(vehicleModel, watch.getThresholdPercent());
        if (!changes.isEmpty()) {
            mailService.sendPriceAlert(watch.getUser(), vehicleModel, changes);
        }
    }

    private List<PriceChange> significantChanges(VehicleModel vehicleModel, BigDecimal thresholdPercent) {
        List<PriceEntry> allEntries = priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(vehicleModel.getId());

        Map<String, List<PriceEntry>> byLine = allEntries.stream()
                .collect(Collectors.groupingBy(pe -> pe.getYearCode() + "|" + pe.getFuelType().getId()));

        List<PriceChange> changes = new ArrayList<>();
        for (List<PriceEntry> line : byLine.values()) {
            List<PriceEntry> sortedByMonthDesc = line.stream()
                    .sorted(Comparator.comparing(PriceEntry::getReferenceMonthKey).reversed())
                    .toList();

            if (sortedByMonthDesc.size() < 2) {
                continue; // combinação nova, sem preço anterior pra comparar
            }

            PriceEntry latest = sortedByMonthDesc.get(0);
            PriceEntry previous = sortedByMonthDesc.get(1);
            if (previous.getPrice().signum() == 0) {
                continue; // evita divisão por zero num dado esquisito
            }

            BigDecimal changePercent = latest.getPrice().subtract(previous.getPrice())
                    .divide(previous.getPrice(), 6, RoundingMode.HALF_UP);

            if (changePercent.abs().compareTo(thresholdPercent) >= 0) {
                changes.add(new PriceChange(latest.getYearCode(), latest.getYearValue(),
                        latest.getFuelType().getName(), previous.getPrice(), latest.getPrice(), changePercent));
            }
        }
        return changes;
    }
}
