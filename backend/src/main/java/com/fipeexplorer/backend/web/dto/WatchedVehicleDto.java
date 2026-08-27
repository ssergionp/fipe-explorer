package com.fipeexplorer.backend.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchedVehicleDto(
        Long id,
        String fipeCode,
        String brand,
        String model,
        BigDecimal thresholdPercent,
        Instant createdAt
) {
}
