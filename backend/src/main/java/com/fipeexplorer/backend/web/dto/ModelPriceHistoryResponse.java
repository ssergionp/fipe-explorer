package com.fipeexplorer.backend.web.dto;

import java.util.List;

public record ModelPriceHistoryResponse(
        Long modelId,
        String brand,
        String model,
        String vehicleType,
        String fipeCode,
        List<PricePointDto> prices
) {
}
