package com.fipeexplorer.backend.importer;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/**
 * Converte o "Month" da Tabela FIPE ("agosto de 2026") pro dia 1 daquele mês. Mapa de nomes
 * hardcoded de propósito - não usa DateTimeFormatter com Locale("pt","BR") porque isso depende do
 * provedor de locale da JVM (ICU vs. COMPAT), que varia entre ambientes.
 */
public final class ReferenceMonthParser {

    private static final Map<String, Integer> MONTH_NUMBERS = Map.ofEntries(
            Map.entry("janeiro", 1),
            Map.entry("fevereiro", 2),
            Map.entry("março", 3),
            Map.entry("abril", 4),
            Map.entry("maio", 5),
            Map.entry("junho", 6),
            Map.entry("julho", 7),
            Map.entry("agosto", 8),
            Map.entry("setembro", 9),
            Map.entry("outubro", 10),
            Map.entry("novembro", 11),
            Map.entry("dezembro", 12));

    private ReferenceMonthParser() {
    }

    public static LocalDate parse(String referenceMonth) {
        if (referenceMonth == null || referenceMonth.isBlank()) {
            throw new IllegalArgumentException("Mês de referência vazio");
        }

        String[] parts = referenceMonth.trim().toLowerCase(Locale.ROOT).split(" de ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Mês de referência em formato inesperado: " + referenceMonth);
        }

        Integer month = MONTH_NUMBERS.get(parts[0]);
        if (month == null) {
            throw new IllegalArgumentException("Mês de referência desconhecido: " + referenceMonth);
        }

        int year;
        try {
            year = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ano inválido no mês de referência: " + referenceMonth, e);
        }

        return LocalDate.of(year, month, 1);
    }
}
