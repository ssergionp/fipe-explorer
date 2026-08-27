package com.fipeexplorer.backend.importer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CsvImportRunner implements CommandLineRunner {

    private final ImportOrchestrator importOrchestrator;

    public CsvImportRunner(ImportOrchestrator importOrchestrator) {
        this.importOrchestrator = importOrchestrator;
    }

    @Override
    public void run(String... args) {
        importOrchestrator.runImportAndCheckAlerts();
    }
}
