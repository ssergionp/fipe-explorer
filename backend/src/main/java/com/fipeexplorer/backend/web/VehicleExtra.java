package com.fipeexplorer.backend.web;

/**
 * Lista curta e pré-definida de opcionais reconhecidos pela calculadora de estimativa de valor
 * (ver {@link com.fipeexplorer.backend.estimate.PriceEstimateService}) — cada um soma um
 * percentual fixo, independente dos outros. Valores são um ponto de partida ajustável.
 */
public enum VehicleExtra {
    AR_CONDICIONADO(0.01, "Ar-condicionado"),
    DIRECAO_HIDRAULICA(0.01, "Direção hidráulica/elétrica"),
    RODAS_LIGA_LEVE(0.015, "Rodas de liga leve"),
    TETO_SOLAR(0.02, "Teto solar"),
    BANCO_COURO(0.02, "Bancos de couro"),
    MULTIMIDIA(0.015, "Central multimídia"),
    BLINDAGEM(0.08, "Blindagem");

    private final double adjustmentPercent;
    private final String label;

    VehicleExtra(double adjustmentPercent, String label) {
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
