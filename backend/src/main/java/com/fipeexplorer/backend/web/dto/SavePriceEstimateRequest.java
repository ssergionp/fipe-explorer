package com.fipeexplorer.backend.web.dto;

import com.fipeexplorer.backend.web.VehicleCondition;
import com.fipeexplorer.backend.web.VehicleExtra;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Sem adjustedPrice/components aqui de propósito: o valor ajustado é sempre recalculado no
 * servidor via PriceEstimateService, nunca aceito do cliente.
 */
public record SavePriceEstimateRequest(
        @NotNull Long priceEntryId,
        @NotNull @PositiveOrZero Long km,
        @NotNull VehicleCondition condition,
        List<VehicleExtra> extras
) {
}
