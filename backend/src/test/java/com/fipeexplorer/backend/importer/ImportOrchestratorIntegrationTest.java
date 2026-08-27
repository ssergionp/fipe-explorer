package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.alerts.PriceAlertService;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.ImportRunRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Prova que os três gatilhos de import (startup, cron, trigger manual) - que só chamam
 * ImportOrchestrator.runImportAndCheckAlerts() - disparam a MESMA checagem de alerta, e só quando
 * algo novo de fato entrou. PriceAlertService é substituído por um mock (@Primary) - o que
 * importa aqui é SE ele é chamado, não o que ele faz internamente (isso já é coberto por
 * PriceAlertServiceTest).
 */
@SpringBootTest
@Tag("integration")
class ImportOrchestratorIntegrationTest {

    private static final Path INCOMING_DIR = createTempDir("fipe-orchestrator-test-incoming");
    private static final Path PROCESSED_DIR = createTempDir("fipe-orchestrator-test-processed");

    @DynamicPropertySource
    static void configureImportPaths(DynamicPropertyRegistry registry) {
        registry.add("fipe.import.csv-path", () -> INCOMING_DIR.resolve("sem-csv-semente-aqui.csv").toString());
        registry.add("fipe.import.incoming-dir", INCOMING_DIR::toString);
        registry.add("fipe.import.processed-dir", PROCESSED_DIR::toString);
    }

    @Autowired
    private ImportOrchestrator orchestrator;

    @Autowired
    private PriceAlertService priceAlertService; // é o mock do TestConfiguration abaixo

    @Autowired
    private ImportRunRepository importRunRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private VehicleModelRepository vehicleModelRepository;
    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @BeforeEach
    void resetMock() {
        // O mock é um bean singleton no contexto Spring cacheado entre os métodos de teste desta
        // classe - sem isso, uma invocação de um teste "vaza" para o verify(never()) do próximo.
        reset(priceAlertService);
    }

    @AfterEach
    void cleanUpDatabase() {
        importRunRepository.findAll().stream()
                .filter(run -> run.getReferenceMonthKey().equals(LocalDate.of(2098, 1, 1)))
                .forEach(importRunRepository::delete);

        brandRepository.findByFipeCode("ORCHTESTFIPE").ifPresent(brand -> {
            vehicleModelRepository.findByBrandIdAndFipeModelCode(brand.getId(), "M1").ifPresent(model -> {
                priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(model.getId())
                        .forEach(priceEntryRepository::delete);
                vehicleModelRepository.delete(model);
            });
            brandRepository.delete(brand);
        });
    }

    private static Path createTempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void nothingNewImportedDoesNotTriggerAlertCheck() {
        orchestrator.runImportAndCheckAlerts(); // incoming vazio, csv semente não existe

        verify(priceAlertService, never()).checkAllWatchedVehicles();
    }

    @Test
    void newFileImportedTriggersAlertCheck() throws IOException {
        String csv = "Type,Brand Code,Brand Value,Model Code,Model Value,Year Code,Year Value,Fipe Code,"
                + "Fuel Letter,Fuel Type,Price,Month\n"
                + "CAR,ORCHTESTFIPE,Marca Teste Orchestrator,M1,Modelo Teste Orchestrator,2020-1,"
                + "2020 Gasolina,777770-0,G,Gasolina,\"R$ 1.234,00\",janeiro de 2098\n";
        Files.writeString(INCOMING_DIR.resolve("teste-orchestrator.csv"), csv, StandardCharsets.UTF_8);

        orchestrator.runImportAndCheckAlerts();

        verify(priceAlertService).checkAllWatchedVehicles();
    }

    @TestConfiguration
    static class MockPriceAlertServiceConfig {
        @Bean
        @Primary
        PriceAlertService mockPriceAlertService() {
            return mock(PriceAlertService.class);
        }
    }
}
