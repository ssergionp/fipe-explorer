package com.fipeexplorer.backend.web.dto;

import com.fipeexplorer.backend.web.VehicleCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SavedPriceEstimateDto(
        Long id,
        VehicleSearchResultDto vehicle,
        long km,
        VehicleCondition condition,
        List<String> extras,
        BigDecimal basePrice,
        BigDecimal adjustedPrice,
        List<PriceAdjustmentComponentDto> components,
        Instant createdAt
) {
}
