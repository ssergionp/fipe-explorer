package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.domain.ImportRun;
import com.fipeexplorer.backend.repository.ImportRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Disparado por três caminhos diferentes - startup (CsvImportRunner), cron mensal
 * (ImportScheduler) e sob demanda (AdminImportController) - todos chamando este mesmo método, o
 * que garante que os três respeitam a mesma idempotência: monta uma lista de candidatos (o CSV
 * semente + qualquer *.csv em data/incoming/) e importa qualquer um cujo mês de referência ainda
 * não esteja em import_run. Dedup é pelo mês, não pelo nome do arquivo - um CSV baixado de novo
 * com outro nome pro mesmo mês não duplica. Um arquivo ruim é logado e pulado, nunca derruba a
 * chamada inteira.
 */
@Component
public class IncomingCsvScanner {

    private static final Logger log = LoggerFactory.getLogger(IncomingCsvScanner.class);

    private final FipeCsvImportService importService;
    private final ImportRunRepository importRunRepository;
    private final Path seedCsvPath;
    private final Path incomingDir;
    private final Path processedDir;

    public IncomingCsvScanner(FipeCsvImportService importService,
                               ImportRunRepository importRunRepository,
                               @Value("${fipe.import.csv-path}") String seedCsvPath,
                               @Value("${fipe.import.incoming-dir}") String incomingDir,
                               @Value("${fipe.import.processed-dir}") String processedDir) {
        this.importService = importService;
        this.importRunRepository = importRunRepository;
        this.seedCsvPath = Path.of(seedCsvPath);
        this.incomingDir = Path.of(incomingDir);
        this.processedDir = Path.of(processedDir);
    }

    /** @return os ImportRun criados nesta chamada (não o histórico inteiro) - vazio se nada era novo. */
    public List<ImportRun> scanAndImport() {
        List<ImportRun> imported = new ArrayList<>();
        for (Path candidate : candidateFiles()) {
            processFile(candidate).ifPresent(imported::add);
        }
        return imported;
    }

    private List<Path> candidateFiles() {
        List<Path> candidates = new ArrayList<>();
        if (Files.isRegularFile(seedCsvPath)) {
            candidates.add(seedCsvPath);
        }
        candidates.addAll(listIncomingCsvFiles());
        return candidates;
    }

    private List<Path> listIncomingCsvFiles() {
        if (!Files.isDirectory(incomingDir)) {
            return List.of();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(incomingDir, "*.csv")) {
            List<Path> files = new ArrayList<>();
            stream.forEach(files::add);
            files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            return files;
        } catch (IOException e) {
            log.warn("Não foi possível listar {}: {}", incomingDir, e.getMessage());
            return List.of();
        }
    }

    private Optional<ImportRun> processFile(Path csvPath) {
        try {
            Optional<String> referenceMonth = importService.peekReferenceMonth(csvPath);
            if (referenceMonth.isEmpty()) {
                log.warn("Ignorando {} - não foi possível determinar o mês de referência "
                        + "(arquivo vazio ou malformado).", csvPath);
                return Optional.empty();
            }

            LocalDate referenceMonthKey;
            try {
                referenceMonthKey = ReferenceMonthParser.parse(referenceMonth.get());
            } catch (IllegalArgumentException e) {
                log.warn("Ignorando {} - mês de referência inválido: {}", csvPath, e.getMessage());
                return Optional.empty();
            }

            if (importRunRepository.existsByReferenceMonthKey(referenceMonthKey)) {
                log.info("Mês de referência {} ({}) já importado, pulando {}.",
                        referenceMonth.get(), referenceMonthKey, csvPath);
                return Optional.empty();
            }

            int rowCount = importService.importCsv(csvPath);
            ImportRun importRun = importRunRepository.save(new ImportRun(
                    csvPath.getFileName().toString(), referenceMonth.get(), referenceMonthKey, rowCount));
            log.info("Importado {}: {} linhas, mês de referência {}.", csvPath, rowCount, referenceMonth.get());

            if (isFromIncomingDir(csvPath)) {
                moveToProcessed(csvPath);
            }
            return Optional.of(importRun);
        } catch (Exception e) {
            // Um CSV ruim não pode derrubar a chamada inteira - loga e segue pro próximo candidato.
            log.error("Falha ao importar {}: {}", csvPath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private boolean isFromIncomingDir(Path csvPath) {
        Path parent = csvPath.getParent();
        return parent != null && parent.normalize().equals(incomingDir.normalize());
    }

    private void moveToProcessed(Path csvPath) {
        try {
            Files.createDirectories(processedDir);
            Files.move(csvPath, processedDir.resolve(csvPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Importado com sucesso, mas não foi possível mover {} para {}: {}",
                    csvPath, processedDir, e.getMessage());
        }
    }
}
