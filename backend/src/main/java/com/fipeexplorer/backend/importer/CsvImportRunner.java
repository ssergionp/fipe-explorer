package com.fipeexplorer.backend.importer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CsvImportRunner implements CommandLineRunner {

    private final IncomingCsvScanner incomingCsvScanner;

    public CsvImportRunner(IncomingCsvScanner incomingCsvScanner) {
        this.incomingCsvScanner = incomingCsvScanner;
    }

    @Override
    public void run(String... args) {
        incomingCsvScanner.scanAndImport();
    }
}
