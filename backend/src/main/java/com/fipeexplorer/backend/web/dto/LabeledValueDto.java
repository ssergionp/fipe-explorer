package com.fipeexplorer.backend.web.dto;

/** Par chave técnica (usada no request) / rótulo em pt-BR (usado na UI) — para popular selects. */
public record LabeledValueDto(
        String key,
        String label
) {
}
