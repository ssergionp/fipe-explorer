package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.ImportRunRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Bate direto no Postgres de dev (docker-compose, localhost:5433) — mesma convenção da maioria
 * dos testes de integração deste projeto (a exceção é FipeCsvImportServiceIntegrationTest, que
 * precisa de um banco vazio de verdade pra contar linhas, então usa Testcontainers). Aqui não
 * precisamos de banco vazio: cada teste usa um mês de referência fictício (ano 2099) e uma marca
 * fictícia, que não colidem com o dado real.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class IncomingCsvScannerIntegrationTest {

    // Inicializado no carregamento da classe - precisa existir ANTES do @DynamicPropertySource
    // rodar (Spring resolve essas propriedades durante a preparação do contexto, antes de
    // qualquer callback de ciclo de vida do JUnit como @BeforeAll).
    private static final Path INCOMING_DIR = createTempDir("fipe-import-test-incoming");
    private static final Path PROCESSED_DIR = createTempDir("fipe-import-test-processed");

    private static final String CSV_HEADER =
            "Type,Brand Code,Brand Value,Model Code,Model Value,Year Code,Year Value,Fipe Code,Fuel Letter,Fuel Type,Price,Month";

    @DynamicPropertySource
    static void configureImportPaths(DynamicPropertyRegistry registry) {
        // Aponta pra um arquivo que nunca existe, pra não reprocessar o CSV semente real durante
        // este teste (ele já está registrado em import_run pelo backfill da migration V9).
        registry.add("fipe.import.csv-path", () -> INCOMING_DIR.resolve("sem-csv-semente-aqui.csv").toString());
        registry.add("fipe.import.incoming-dir", INCOMING_DIR::toString);
        registry.add("fipe.import.processed-dir", PROCESSED_DIR::toString);
    }

    @Autowired
    private IncomingCsvScanner scanner;
    @Autowired
    private ImportRunRepository importRunRepository;
    @Autowired
    private PriceEntryRepository priceEntryRepository;
    @Autowired
    private VehicleModelRepository vehicleModelRepository;
    @Autowired
    private BrandRepository brandRepository;

    @AfterAll
    void cleanUpDatabase() {
        importRunRepository.findAll().stream()
                .filter(run -> run.getReferenceMonthKey().getYear() == 2099)
                .forEach(importRunRepository::delete);

        brandRepository.findByFipeCode("TESTFIPE1").ifPresent(brand -> {
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

    private void writeCsv(String filename, String referenceMonth, String... yearCodes) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (String yearCode : yearCodes) {
            csv.append("CAR,TESTFIPE1,Marca Teste Importador,M1,Modelo Teste Importador,")
                    .append(yearCode).append(',').append(yearCode).append(" Gasolina,")
                    .append("999999-9,G,Gasolina,\"R$ 1.234,00\",").append(referenceMonth).append('\n');
        }
        try {
            Files.writeString(INCOMING_DIR.resolve(filename), csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<PriceEntry> priceEntriesForTestModel() {
        Brand brand = brandRepository.findByFipeCode("TESTFIPE1").orElseThrow();
        Long modelId = vehicleModelRepository.findByBrandIdAndFipeModelCode(brand.getId(), "M1")
                .orElseThrow().getId();
        return priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(modelId);
    }

    @Test
    void newFileIsImportedAndRegistered() {
        writeCsv("teste-2099-01.csv", "janeiro de 2099", "2020-1", "2021-1");

        scanner.scanAndImport();

        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 1, 1))).isTrue();
        assertThat(Files.exists(INCOMING_DIR.resolve("teste-2099-01.csv"))).isFalse();
        assertThat(Files.exists(PROCESSED_DIR.resolve("teste-2099-01.csv"))).isTrue();

        long countForJan2099 = priceEntriesForTestModel().stream()
                .filter(p -> p.getReferenceMonth().equals("janeiro de 2099"))
                .count();
        assertThat(countForJan2099).isEqualTo(2);
    }

    @Test
    void sameReferenceMonthUnderADifferentFilenameIsNotReimported() {
        writeCsv("original-2099-02.csv", "fevereiro de 2099", "2020-1", "2021-1");
        scanner.scanAndImport();
        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 2, 1))).isTrue();

        long countAfterFirstImport = priceEntriesForTestModel().stream()
                .filter(p -> p.getReferenceMonth().equals("fevereiro de 2099"))
                .count();
        assertThat(countAfterFirstImport).isEqualTo(2);

        // Mesmo mês, arquivo com nome diferente - dedup é pelo mês, não pelo nome do arquivo.
        writeCsv("re-baixado-fevereiro-2099.csv", "fevereiro de 2099", "2020-1", "2021-1");
        scanner.scanAndImport();

        long countAfterSecondAttempt = priceEntriesForTestModel().stream()
                .filter(p -> p.getReferenceMonth().equals("fevereiro de 2099"))
                .count();
        assertThat(countAfterSecondAttempt).isEqualTo(2); // não dobrou

        long importRunRowsForFeb2099 = importRunRepository.findAll().stream()
                .filter(run -> run.getReferenceMonthKey().equals(LocalDate.of(2099, 2, 1)))
                .count();
        assertThat(importRunRowsForFeb2099).isEqualTo(1);
    }

    @Test
    void malformedFileWithUnreadableHeaderDoesNotCrashScanAndValidFileStillImports() throws IOException {
        Files.writeString(INCOMING_DIR.resolve("corrompido.csv"), "isto,não,é,um,csv,da,fipe\nlixo,lixo,lixo\n",
                StandardCharsets.UTF_8);
        writeCsv("teste-2099-03.csv", "março de 2099", "2020-1");

        assertThatCode(() -> scanner.scanAndImport()).doesNotThrowAnyException();

        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 3, 1))).isTrue();
        // arquivo malformado não é registrado nem movido - continua visível em "incoming" como pendente.
        assertThat(importRunRepository.findAll().stream().anyMatch(run -> run.getFilename().equals("corrompido.csv")))
                .isFalse();
        assertThat(Files.exists(INCOMING_DIR.resolve("corrompido.csv"))).isTrue();
    }

    @Test
    void fileWithBadPriceValueFailsImportWithoutCrashingAndIsNotRegistered() throws IOException {
        String badRow = CSV_HEADER + "\n"
                + "CAR,TESTFIPE1,Marca Teste Importador,M1,Modelo Teste Importador,2020-1,2020-1 Gasolina,"
                + "999999-9,G,Gasolina,preço-invalido,abril de 2099\n";
        Files.writeString(INCOMING_DIR.resolve("preco-invalido.csv"), badRow, StandardCharsets.UTF_8);

        assertThatCode(() -> scanner.scanAndImport()).doesNotThrowAnyException();

        assertThat(importRunRepository.existsByReferenceMonthKey(LocalDate.of(2099, 4, 1))).isFalse();
        assertThat(Files.exists(INCOMING_DIR.resolve("preco-invalido.csv"))).isTrue();
    }
}
