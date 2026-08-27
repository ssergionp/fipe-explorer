package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.ImportRun;
import com.fipeexplorer.backend.importer.ImportOrchestrator;
import com.fipeexplorer.backend.web.dto.ImportTriggerResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dispara o mesmo fluxo do startup e do cron mensal (ImportScheduler) sob demanda, sem esperar o
 * cron nem reiniciar o app - incluindo a checagem de alertas de preço, já que os três gatilhos
 * passam pelo mesmo ImportOrchestrator. Protegido por autenticação (ver SecurityConfig) - não
 * existe role de admin neste projeto (decisão de não usar RBAC ainda), então "protegido" aqui
 * significa "qualquer usuário logado", não um papel específico.
 */
@RestController
@RequestMapping("/api/v1/admin/import")
public class AdminImportController {

    private final ImportOrchestrator importOrchestrator;

    public AdminImportController(ImportOrchestrator importOrchestrator) {
        this.importOrchestrator = importOrchestrator;
    }

    @PostMapping("/trigger")
    public ImportTriggerResponse trigger() {
        List<ImportRun> imported = importOrchestrator.runImportAndCheckAlerts();
        List<ImportTriggerResponse.ImportedFileDto> importedDtos = imported.stream()
                .map(run -> new ImportTriggerResponse.ImportedFileDto(
                        run.getFilename(), run.getReferenceMonth(), run.getRowCount()))
                .toList();
        return new ImportTriggerResponse(importedDtos.size(), importedDtos);
    }
}
