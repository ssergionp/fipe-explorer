package com.fipeexplorer.backend.web.dto;

import com.fipeexplorer.backend.domain.PriceEntry;

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

    /** Único lugar que sabe montar esse shape a partir de um PriceEntry - busca, favoritos e
     * estimativas salvas reaproveitam este mesmo factory em vez de duplicar o mapeamento. */
    public static VehicleSearchResultDto from(PriceEntry entry) {
        return new VehicleSearchResultDto(
                entry.getId(),
                entry.getVehicleModel().getId(),
                entry.getVehicleModel().getBrand().getName(),
                entry.getVehicleModel().getName(),
                yearFromYearCode(entry.getYearCode()),
                entry.getFuelType().getName(),
                entry.getPrice(),
                entry.getVehicleModel().getFipePriceCode());
    }

    private static String yearFromYearCode(String yearCode) {
        int dashIndex = yearCode.indexOf('-');
        return dashIndex >= 0 ? yearCode.substring(0, dashIndex) : yearCode;
    }
}
