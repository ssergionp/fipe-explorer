package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record PricePointDto(
        String yearCode,
        String yearValue,
        String fuel,
        BigDecimal price
) {
}
