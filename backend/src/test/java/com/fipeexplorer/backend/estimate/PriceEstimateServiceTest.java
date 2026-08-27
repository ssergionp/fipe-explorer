package com.fipeexplorer.backend.estimate;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.FuelType;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.VehicleModel;
import com.fipeexplorer.backend.web.VehicleCondition;
import com.fipeexplorer.backend.web.VehicleExtra;
import com.fipeexplorer.backend.web.dto.PriceAdjustmentComponentDto;
import com.fipeexplorer.backend.web.dto.PriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.PriceEstimateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceEstimateServiceTest {

    // Congela "hoje" em 2026 (bate com o ano de referência dos fixtures deste teste) — a
    // calculadora usa o relógio pra achar a idade do veículo a partir do year_code.
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);

    private final PriceEstimateService service = new PriceEstimateService(FIXED_CLOCK);
    private final Brand brand = new Brand("21", "Fiat");
    private final FuelType gasolina = new FuelType("G", "Gasolina");

    private PriceEntry priceEntry(String yearCode, String price) {
        VehicleModel model = new VehicleModel(brand, "469", "Elba CS 1.6", "CAR", "001023-5");
        return new PriceEntry(model, gasolina, yearCode, "veículo de teste", new BigDecimal(price),
                "agosto de 2026", java.time.LocalDate.of(2026, 8, 1));
    }

    private PriceEstimateRequest request(long km, VehicleCondition condition, VehicleExtra... extras) {
        return new PriceEstimateRequest(km, condition, List.of(extras));
    }

    private PriceAdjustmentComponentDto component(PriceEstimateResponse response, String key) {
        return response.components().stream()
                .filter(c -> c.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Componente " + key + " não encontrado em " + response.components()));
    }

    // --- Quilometragem ---

    @Test
    void mileageWithinExpectedRangeHasNoAdjustment() {
        // 2021, 5 anos de idade em 2026 -> esperado 60.000 km; 60.000 informado = exatamente no esperado.
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(60_000, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isZero();
    }

    @Test
    void veryLowMileageGetsMaximumBonus() {
        // Mesmo carro, mas rodou muito pouco (33% do esperado) -> faixa "muito abaixo do esperado".
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(20_000, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isEqualTo(0.05);
    }

    @Test
    void veryHighMileageIsCappedAtWorstBand() {
        // 250% do esperado -> não desconta além do teto de -15%.
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(150_000, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isEqualTo(-0.15);
    }

    @Test
    void moderatelyHighMileageGetsIntermediateDiscount() {
        // 125% do esperado -> faixa "acima do esperado" (-5%), não a pior faixa.
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(75_000, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isEqualTo(-0.05);
    }

    @Test
    void zeroKmVehicleWithZeroKmInformedHasNoAdjustment() {
        PriceEstimateResponse response = service.estimate(priceEntry("32000", "80000.00"), request(0, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isZero();
    }

    @Test
    void zeroKmVehicleWithAnyMileageInformedGetsWorstBand() {
        // Se o veículo é anunciado como zero km, qualquer km rodado já é "muito acima do esperado".
        PriceEstimateResponse response = service.estimate(priceEntry("32000", "80000.00"), request(500, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isEqualTo(-0.15);
    }

    @Test
    void veryOldVehicleWithRealisticMileageStillGetsSensibleBonusNotACrash() {
        // 1970 -> 56 anos de idade em 2026, esperado = 672.000 km. Km realista (150.000) fica bem
        // abaixo disso -> maior faixa de bônus, sem overflow nem comportamento estranho.
        PriceEstimateResponse response = service.estimate(priceEntry("1970-1", "5000.00"), request(150_000, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isEqualTo(0.05);
    }

    @Test
    void futureModelYearClampsAgeToZeroInsteadOfNegative() {
        // year_code no futuro (não deveria acontecer, mas não pode gerar km esperado negativo).
        PriceEstimateResponse response = service.estimate(priceEntry("2030-1", "90000.00"), request(0, VehicleCondition.BOM));

        assertThat(component(response, "MILEAGE").percent()).isZero();
    }

    // --- Estado de conservação ---

    @ParameterizedTest
    @CsvSource({
            "EXCELENTE, 0.05",
            "BOM, 0.0",
            "REGULAR, -0.10",
            "RUIM, -0.20",
    })
    void conditionAppliesItsFixedPercent(VehicleCondition condition, double expectedPercent) {
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(60_000, condition));

        assertThat(component(response, "CONDITION").percent()).isEqualTo(expectedPercent);
    }

    // --- Opcionais ---

    @Test
    void noExtrasProducesOnlyMileageAndConditionComponents() {
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"), request(60_000, VehicleCondition.BOM));

        assertThat(response.components()).hasSize(2);
    }

    @Test
    void multipleExtrasEachAddTheirOwnComponentAndSumIndependently() {
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"),
                request(60_000, VehicleCondition.BOM, VehicleExtra.AR_CONDICIONADO, VehicleExtra.TETO_SOLAR));

        assertThat(component(response, "EXTRA:AR_CONDICIONADO").percent()).isEqualTo(0.01);
        assertThat(component(response, "EXTRA:TETO_SOLAR").percent()).isEqualTo(0.02);
        assertThat(response.components()).hasSize(4); // km + condição + 2 opcionais
    }

    // --- Consistência do total / arredondamento ---

    @Test
    void adjustedPriceEqualsBasePricePlusSumOfComponentAmounts() {
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "60000.00"),
                request(150_000, VehicleCondition.RUIM, VehicleExtra.BLINDAGEM, VehicleExtra.MULTIMIDIA));

        BigDecimal sumOfComponents = response.components().stream()
                .map(PriceAdjustmentComponentDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(response.adjustedPrice()).isEqualByComparingTo(response.basePrice().add(sumOfComponents));
    }

    @Test
    void componentAmountRoundsToTwoDecimalPlaces() {
        // 333.33 * 1.5% = 4.99995 -> arredonda pra 5.00.
        PriceEstimateResponse response = service.estimate(priceEntry("2021-1", "333.33"),
                request(60_000, VehicleCondition.BOM, VehicleExtra.RODAS_LIGA_LEVE));

        assertThat(component(response, "EXTRA:RODAS_LIGA_LEVE").amount()).isEqualByComparingTo(new BigDecimal("5.00"));
    }
}
