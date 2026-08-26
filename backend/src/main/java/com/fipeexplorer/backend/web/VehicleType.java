package com.fipeexplorer.backend.web;

public enum VehicleType {
    CAR("cars"),
    MOTORCYCLE("motorcycles"),
    TRUCK("trucks");

    private final String externalApiSegment;

    VehicleType(String externalApiSegment) {
        this.externalApiSegment = externalApiSegment;
    }

    /**
     * Segmento de path usado pela API pública da FIPE (fipe.parallelum.com.br/api/v2), que nomeia
     * tipos de veículo em inglês e no plural, diferente do nosso enum.
     */
    public String externalApiSegment() {
        return externalApiSegment;
    }
}
