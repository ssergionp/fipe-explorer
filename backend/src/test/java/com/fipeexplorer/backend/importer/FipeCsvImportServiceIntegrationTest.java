package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.FuelTypeRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o contexto Spring completo (Flyway + CsvImportRunner) contra um Postgres descartável e
 * confere se a importação do CSV real da Tabela FIPE bate com as contagens conhecidas do arquivo
 * (238 marcas, 7 combustíveis, 11.357 modelos, 50.838 registros de preço).
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
class FipeCsvImportServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fipe_explorer_test")
            .withUsername("fipe")
            .withPassword("fipe");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private FuelTypeRepository fuelTypeRepository;
    @Autowired
    private VehicleModelRepository vehicleModelRepository;
    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @Test
    void importsFullFipeCsvOnStartup() {
        assertThat(brandRepository.count()).isEqualTo(238);
        assertThat(fuelTypeRepository.count()).isEqualTo(7);
        assertThat(vehicleModelRepository.count()).isEqualTo(11357);
        assertThat(priceEntryRepository.count()).isEqualTo(50838);
    }
}
