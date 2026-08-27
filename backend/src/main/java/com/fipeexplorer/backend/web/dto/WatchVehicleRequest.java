package com.fipeexplorer.backend.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * thresholdPercent é opcional - omitido, o serviço aplica o default de 5% (0.05). Quando
 * informado, é fração (0.05 = 5%), não um inteiro de porcentagem, mesma convenção do resto do
 * projeto (VehicleCondition/VehicleExtra).
 */
public record WatchVehicleRequest(
        @NotBlank String fipeCode,

        @DecimalMin(value = "0.01", message = "O threshold precisa ser de pelo menos 1%")
        @DecimalMax(value = "1.00", message = "O threshold não pode passar de 100%")
        BigDecimal thresholdPercent
) {
}
