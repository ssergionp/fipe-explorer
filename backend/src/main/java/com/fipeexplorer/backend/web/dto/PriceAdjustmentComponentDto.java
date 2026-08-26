package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record PriceAdjustmentComponentDto(
        String key,
        String label,
        double percent,
        BigDecimal amount
) {
}
