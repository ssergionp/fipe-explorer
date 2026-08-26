package com.fipeexplorer.backend.web.dto;

import com.fipeexplorer.backend.web.VehicleCondition;
import com.fipeexplorer.backend.web.VehicleExtra;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record PriceEstimateRequest(
        @NotNull @PositiveOrZero Long km,
        @NotNull VehicleCondition condition,
        List<VehicleExtra> extras
) {
}
