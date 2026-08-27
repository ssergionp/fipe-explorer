package com.fipeexplorer.backend.web.dto;

import java.util.List;

public record ImportTriggerResponse(
        int importedFileCount,
        List<ImportedFileDto> imported
) {

    public record ImportedFileDto(
            String filename,
            String referenceMonth,
            int rowCount
    ) {
    }
}
