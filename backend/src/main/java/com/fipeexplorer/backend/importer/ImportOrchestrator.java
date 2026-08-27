package com.fipeexplorer.backend.importer;

import com.fipeexplorer.backend.alerts.PriceAlertService;
import com.fipeexplorer.backend.domain.ImportRun;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ponto único chamado pelos três gatilhos de import (CsvImportRunner no startup, ImportScheduler
 * no cron mensal, AdminImportController sob demanda) - garante que os três disparam exatamente a
 * mesma sequência: importa, e só se algo novo entrou, checa alertas de preço dos veículos
 * observados. Nenhum dos três chama IncomingCsvScanner ou PriceAlertService diretamente.
 */
@Component
public class ImportOrchestrator {

    private final IncomingCsvScanner incomingCsvScanner;
    private final PriceAlertService priceAlertService;

    public ImportOrchestrator(IncomingCsvScanner incomingCsvScanner, PriceAlertService priceAlertService) {
        this.incomingCsvScanner = incomingCsvScanner;
        this.priceAlertService = priceAlertService;
    }

    public List<ImportRun> runImportAndCheckAlerts() {
        List<ImportRun> imported = incomingCsvScanner.scanAndImport();
        if (!imported.isEmpty()) {
            priceAlertService.checkAllWatchedVehicles();
        }
        return imported;
    }
}
