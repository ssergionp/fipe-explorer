package com.fipeexplorer.backend.web;

/**
 * Estado de conservação — um dos três componentes da calculadora de estimativa de valor
 * (ver {@link com.fipeexplorer.backend.estimate.PriceEstimateService}).
 *
 * <p>Percentuais são um ponto de partida ajustável, não uma verdade absoluta: "Bom" é a
 * referência (0%, é aproximadamente o estado médio que a Tabela FIPE já assume implicitamente),
 * e os demais se afastam dela pra cima ou pra baixo.
 */
public enum VehicleCondition {
    EXCELENTE(0.05, "Excelente"),
    BOM(0.0, "Bom"),
    REGULAR(-0.10, "Regular"),
    RUIM(-0.20, "Ruim");

    private final double adjustmentPercent;
    private final String label;

    VehicleCondition(double adjustmentPercent, String label) {
        this.adjustmentPercent = adjustmentPercent;
        this.label = label;
    }

    public double adjustmentPercent() {
        return adjustmentPercent;
    }

    public String label() {
        return label;
    }
}
