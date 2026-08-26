package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record PricePointDto(
        Long priceEntryId,
        String yearCode,
        String yearValue,
        String fuel,
        BigDecimal price
) {
}
