package com.fipeexplorer.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.importer.IncomingCsvScanner;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.ImportRunRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.RefreshTokenRepository;
import com.fipeexplorer.backend.repository.UserRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev, mesmo padrão de IncomingCsvScannerIntegrationTest — mês de
 * referência e marca fictícios (ano 2099) pra não colidir com dado real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class AdminImportControllerIntegrationTest {

    private static final String EMAIL_DOMAIN = "@admin-import-integration-test.example.com";

    private static final Path INCOMING_DIR = createTempDir("fipe-admin-import-test-incoming");
    private static final Path PROCESSED_DIR = createTempDir("fipe-admin-import-test-processed");

    private static final String CSV_HEADER =
            "Type,Brand Code,Brand Value,Model Code,Model Value,Year Code,Year Value,Fipe Code,Fuel Letter,Fuel Type,Price,Month";

    @DynamicPropertySource
    static void configureImportPaths(DynamicPropertyRegistry registry) {
        registry.add("fipe.import.csv-path", () -> INCOMING_DIR.resolve("sem-csv-semente-aqui.csv").toString());
        registry.add("fipe.import.incoming-dir", INCOMING_DIR::toString);
        registry.add("fipe.import.processed-dir", PROCESSED_DIR::toString);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IncomingCsvScanner incomingCsvScanner;
    @Autowired
    private ImportRunRepository importRunRepository;
    @Autowired
    private PriceEntryRepository priceEntryRepository;
    @Autowired
    private VehicleModelRepository vehicleModelRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterAll
    void cleanUpDatabase() {
        importRunRepository.findAll().stream()
                .filter(run -> run.getReferenceMonthKey().getYear() == 2099)
                .forEach(importRunRepository::delete);

        brandRepository.findByFipeCode("ADMINTESTFIPE").ifPresent(brand -> {
            vehicleModelRepository.findByBrandIdAndFipeModelCode(brand.getId(), "M1").ifPresent(model -> {
                priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(model.getId())
                        .forEach(priceEntryRepository::delete);
                vehicleModelRepository.delete(model);
            });
            brandRepository.delete(brand);
        });

        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(EMAIL_DOMAIN))
                .forEach(u -> {
                    refreshTokenRepository.deleteByUser(u);
                    userRepository.delete(u);
                });
    }

    private static Path createTempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeCsv(String filename, String referenceMonth, String... yearCodes) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (String yearCode : yearCodes) {
            csv.append("CAR,ADMINTESTFIPE,Marca Teste Admin Import,M1,Modelo Teste Admin Import,")
                    .append(yearCode).append(',').append(yearCode).append(" Gasolina,")
                    .append("888888-8,G,Gasolina,\"R$ 1.234,00\",").append(referenceMonth).append('\n');
        }
        try {
            Files.writeString(INCOMING_DIR.resolve(filename), csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long priceEntryCountForMonth(String referenceMonth) {
        Brand brand = brandRepository.findByFipeCode("ADMINTESTFIPE").orElseThrow();
        Long modelId = vehicleModelRepository.findByBrandIdAndFipeModelCode(brand.getId(), "M1")
                .orElseThrow().getId();
        List<PriceEntry> entries = priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(modelId);
        return entries.stream().filter(p -> p.getReferenceMonth().equals(referenceMonth)).count();
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email": "%s", "password": "senha1234", "acceptedPrivacyPolicy": true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void triggerEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import/trigger"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerEndpointImportsNewFileAndReturnsSummary() throws Exception {
        String token = registerAndGetToken("rita" + EMAIL_DOMAIN);
        writeCsv("teste-2099-05.csv", "maio de 2099", "2020-1", "2021-1");

        mockMvc.perform(post("/api/v1/admin/import/trigger").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedFileCount", is(1)))
                .andExpect(jsonPath("$.imported[0].referenceMonth", is("maio de 2099")))
                .andExpect(jsonPath("$.imported[0].rowCount", is(2)));

        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 5, 1))).isTrue();
    }

    /**
     * O cenário pedido: uma execução "cron" (chamada direta ao IncomingCsvScanner, o mesmo método
     * que o @Scheduled usa) seguida de uma execução "trigger manual" (o endpoint HTTP) pro MESMO
     * mês não podem duplicar price_entry - os dois caminhos reaproveitam a mesma checagem contra
     * import_run.
     */
    @Test
    void cronRunFollowedByManualTriggerForTheSameMonthDoesNotDuplicate() throws Exception {
        String token = registerAndGetToken("sofia" + EMAIL_DOMAIN);

        // "Cron": chamada direta ao scanner, como o ImportScheduler faria.
        writeCsv("cron-2099-06.csv", "junho de 2099", "2020-1", "2021-1");
        incomingCsvScanner.scanAndImport();

        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 6, 1))).isTrue();
        assertThat(priceEntryCountForMonth("junho de 2099")).isEqualTo(2);

        // "Trigger manual": mesmo mês, arquivo novo com outro nome (ex.: alguém baixou de novo achando
        // que não tinha sido processado ainda).
        writeCsv("manual-2099-06.csv", "junho de 2099", "2020-1", "2021-1");

        mockMvc.perform(post("/api/v1/admin/import/trigger").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedFileCount", is(0))); // nada novo pro trigger importar

        assertThat(priceEntryCountForMonth("junho de 2099")).isEqualTo(2); // não dobrou
        long importRunRowsForJune2099 = importRunRepository.findAll().stream()
                .filter(run -> run.getReferenceMonthKey().equals(LocalDate.of(2099, 6, 1)))
                .count();
        assertThat(importRunRowsForJune2099).isEqualTo(1);
    }
}
