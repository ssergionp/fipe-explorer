package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;

public record VehicleSearchResultDto(
        Long id,
        Long modelId,
        String brand,
        String model,
        String year,
        String fuel,
        BigDecimal price,
        String fipeCode
) {
}
