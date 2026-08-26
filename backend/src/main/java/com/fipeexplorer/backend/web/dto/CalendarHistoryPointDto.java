package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record CalendarHistoryPointDto(
        String month,
        String referenceCode,
        BigDecimal price
) {
}
