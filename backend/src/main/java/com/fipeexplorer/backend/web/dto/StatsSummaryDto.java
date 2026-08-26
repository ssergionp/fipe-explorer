package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record StatsSummaryDto(
        long totalPriceEntries,
        long distinctModels,
        BigDecimal minPrice,
        BigDecimal avgPrice,
        BigDecimal maxPrice
) {
}
