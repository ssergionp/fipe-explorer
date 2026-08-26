package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceEstimateResponse(
        BigDecimal basePrice,
        BigDecimal adjustedPrice,
        List<PriceAdjustmentComponentDto> components
) {
}
