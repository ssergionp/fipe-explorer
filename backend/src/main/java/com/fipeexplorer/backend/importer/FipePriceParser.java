package com.fipeexplorer.backend.importer;

import java.math.BigDecimal;

/**
 * Converte o formato de preço da Tabela FIPE ("R$ 10.063,00") para BigDecimal.
 */
public final class FipePriceParser {

    private FipePriceParser() {
    }

    public static BigDecimal parse(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            throw new IllegalArgumentException("Preço vazio");
        }
        String normalized = rawPrice
                .replace("R$", "")
                .trim()
                .replace(".", "")
                .replace(",", ".");
        return new BigDecimal(normalized);
    }
}
