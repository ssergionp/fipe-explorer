package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record TopBrandDto(
        Long brandId,
        String brandName,
        BigDecimal avgPrice,
        long modelCount
) {
}
