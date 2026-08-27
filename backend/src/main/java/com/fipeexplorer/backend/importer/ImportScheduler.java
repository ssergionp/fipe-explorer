package com.fipeexplorer.backend.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparo mensal do mesmo fluxo que o startup (CsvImportRunner) e o endpoint sob demanda
 * (AdminImportController) usam - a checagem de startup continua útil pra ambientes que reiniciam
 * com frequência, mas não é mais o único jeito de disparar um import (um deploy que fica no ar o
 * mês inteiro sem reiniciar agora também pega CSVs novos soltos em data/incoming/).
 */
@Component
public class ImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImportScheduler.class);

    private final IncomingCsvScanner incomingCsvScanner;

    public ImportScheduler(IncomingCsvScanner incomingCsvScanner) {
        this.incomingCsvScanner = incomingCsvScanner;
    }

    @Scheduled(cron = "${fipe.import.schedule.cron}")
    public void scanAndImport() {
        log.info("Disparando scan agendado de CSVs de importação.");
        incomingCsvScanner.scanAndImport();
    }
}
