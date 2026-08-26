package com.fipeexplorer.backend.web.dto;

import java.util.List;

/**
 * Sempre devolvida com HTTP 200 pelo /calendar-history — 404/429/falha de rede da API externa da
 * FIPE viram {@code status} + {@code reason} em vez de propagar o erro HTTP, pra o frontend não
 * precisar distinguir "bug nosso" de "indisponibilidade esperada da fonte externa".
 */
public record CalendarHistoryResponse(
        Status status,
        String reason,
        boolean cached,
        List<CalendarHistoryPointDto> months
) {

    public enum Status {
        AVAILABLE,
        NOT_FOUND,
        RATE_LIMITED,
        UNAVAILABLE
    }
}
