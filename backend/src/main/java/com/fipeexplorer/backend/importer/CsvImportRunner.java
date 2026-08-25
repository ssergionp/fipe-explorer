package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.repository.PriceEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class CsvImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvImportRunner.class);

    private final FipeCsvImportService importService;
    private final PriceEntryRepository priceEntryRepository;
    private final Path csvPath;

    public CsvImportRunner(FipeCsvImportService importService,
                            PriceEntryRepository priceEntryRepository,
                            @Value("${fipe.import.csv-path}") String csvPath) {
        this.importService = importService;
        this.priceEntryRepository = priceEntryRepository;
        this.csvPath = Path.of(csvPath);
    }

    @Override
    public void run(String... args) {
        if (priceEntryRepository.count() > 0) {
            log.info("price_entry já contém dados, pulando importação do CSV.");
            return;
        }
        importService.importCsv(csvPath);
    }
}
