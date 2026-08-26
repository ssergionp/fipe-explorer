package com.fipeexplorer.backend.estimate;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.web.VehicleCondition;
import com.fipeexplorer.backend.web.VehicleExtra;
import com.fipeexplorer.backend.web.dto.PriceAdjustmentComponentDto;
import com.fipeexplorer.backend.web.dto.PriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.PriceEstimateResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calculadora de "valor real estimado": ajusta o preço médio da Tabela FIPE (que reflete
 * modelo/ano, não o carro específico) por três fatores independentes — quilometragem, estado de
 * conservação e opcionais. Cada um vira um percentual sobre o preço base; os percentuais são
 * somados (não compostos) e cada componente aparece separado na resposta, pra quem usa entender
 * de onde veio o número final (valor do componente em R$ = preço base × percentual do componente).
 *
 * <p>Regra de negócio inventada pra este produto, não uma fórmula com resposta "correta" — os
 * números abaixo ({@code ASSUMED_KM_PER_YEAR}, as faixas de km, e os percentuais em
 * {@link VehicleCondition}/{@link VehicleExtra}) são um ponto de partida ajustável, documentado
 * com o raciocínio completo no README.
 */
@Service
public class PriceEstimateService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** Uso moderado assumido pra calcular a quilometragem "esperada" pra idade do veículo. */
    private static final long ASSUMED_KM_PER_YEAR = 12_000;

    /** Convenção FIPE pra zero km (ver PriceEntryRepository) — sem "idade", km esperado é 0. */
    private static final int ZERO_KM_YEAR = 32000;

    /**
     * Faixas de (km atual / km esperado), em ordem crescente de razão — a primeira cujo
     * {@code maxRatio} cobre a razão calculada é usada. "Progressivo": quanto mais longe do
     * esperado, maior o ajuste, mas em degraus (mais fácil de explicar e de ajustar depois que uma
     * curva contínua).
     */
    private record MileageBand(double maxRatio, double adjustmentPercent, String description) {
    }

    private static final List<MileageBand> MILEAGE_BANDS = List.of(
            new MileageBand(0.50, 0.05, "muito abaixo do esperado"),
            new MileageBand(0.90, 0.02, "abaixo do esperado"),
            new MileageBand(1.10, 0.00, "dentro do esperado"),
            new MileageBand(1.50, -0.05, "acima do esperado"),
            new MileageBand(2.00, -0.10, "bem acima do esperado"),
            new MileageBand(Double.POSITIVE_INFINITY, -0.15, "muito acima do esperado"));

    private final Clock clock;

    public PriceEstimateService(Clock clock) {
        this.clock = clock;
    }

    public PriceEstimateResponse estimate(PriceEntry priceEntry, PriceEstimateRequest request) {
        BigDecimal basePrice = priceEntry.getPrice();
        List<VehicleExtra> extras = request.extras() == null ? List.of() : request.extras();

        List<PriceAdjustmentComponentDto> components = new ArrayList<>();
        components.add(mileageComponent(priceEntry, request.km(), basePrice));
        components.add(conditionComponent(request.condition(), basePrice));
        for (VehicleExtra extra : extras) {
            components.add(extraComponent(extra, basePrice));
        }

        BigDecimal totalAdjustment = components.stream()
                .map(PriceAdjustmentComponentDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adjustedPrice = basePrice.add(totalAdjustment).setScale(2, RoundingMode.HALF_UP);

        return new PriceEstimateResponse(basePrice, adjustedPrice, components);
    }

    private PriceAdjustmentComponentDto mileageComponent(PriceEntry priceEntry, long actualKm, BigDecimal basePrice) {
        long expectedKm = expectedKm(priceEntry.getYearCode());
        double ratio = expectedKm == 0
                ? (actualKm == 0 ? 1.0 : Double.POSITIVE_INFINITY)
                : actualKm / (double) expectedKm;

        MileageBand band = MILEAGE_BANDS.stream()
                .filter(b -> ratio <= b.maxRatio())
                .findFirst()
                .orElseThrow();

        String label = String.format(PT_BR, "Quilometragem: %,d km rodados (esperado ~%,d km) — %s",
                actualKm, expectedKm, band.description());

        return component("MILEAGE", label, band.adjustmentPercent(), basePrice);
    }

    private long expectedKm(String yearCode) {
        int vehicleYear = extractYear(yearCode);
        if (vehicleYear == ZERO_KM_YEAR) {
            return 0;
        }
        int currentYear = Year.now(clock).getValue();
        long ageYears = Math.max(currentYear - vehicleYear, 0);
        return ageYears * ASSUMED_KM_PER_YEAR;
    }

    private static int extractYear(String yearCode) {
        return Integer.parseInt(yearCode.split("-")[0]);
    }

    private PriceAdjustmentComponentDto conditionComponent(VehicleCondition condition, BigDecimal basePrice) {
        String label = "Estado de conservação: " + condition.label();
        return component("CONDITION", label, condition.adjustmentPercent(), basePrice);
    }

    private PriceAdjustmentComponentDto extraComponent(VehicleExtra extra, BigDecimal basePrice) {
        return component("EXTRA:" + extra.name(), extra.label(), extra.adjustmentPercent(), basePrice);
    }

    private PriceAdjustmentComponentDto component(String key, String label, double percent, BigDecimal basePrice) {
        BigDecimal amount = basePrice
                .multiply(BigDecimal.valueOf(percent))
                .setScale(2, RoundingMode.HALF_UP);
        return new PriceAdjustmentComponentDto(key, label, percent, amount);
    }
}
