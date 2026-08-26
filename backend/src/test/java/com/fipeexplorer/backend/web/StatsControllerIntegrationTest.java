package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.repository.BrandAveragePriceProjection;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.VehicleModelRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev (docker-compose, localhost:5433) já populado pelo import real
 * do CSV da Tabela FIPE. Não usa Testcontainers (incompatível com o Docker Desktop desta
 * máquina no Windows) — requer apenas `docker compose up -d` previamente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class StatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void summaryReturnsCountsAndPriceRangeForCars() throws Exception {
        mockMvc.perform(get("/api/v1/stats/summary").param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPriceEntries", greaterThan(0)))
                .andExpect(jsonPath("$.distinctModels", greaterThan(0)))
                .andExpect(jsonPath("$.minPrice", greaterThan(0.0)))
                .andExpect(jsonPath("$.avgPrice", greaterThan(0.0)))
                .andExpect(jsonPath("$.maxPrice", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void summaryWithoutTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stats/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topBrandsDescOrdersMostExpensiveFirst() throws Exception {
        mockMvc.perform(get("/api/v1/stats/top-brands")
                        .param("type", "CAR")
                        .param("order", "desc")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(10)))
                .andExpect(jsonPath("$[0].avgPrice", greaterThanOrEqualTo(0.0)));

        // ordem estritamente não-crescente
        var result = mockMvc.perform(get("/api/v1/stats/top-brands")
                        .param("type", "CAR")
                        .param("order", "desc")
                        .param("limit", "10"))
                .andReturn();
        List<Double> prices = extractAvgPrices(result.getResponse().getContentAsString());
        assertThat(prices).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void topBrandsAscOrdersCheapestFirst() throws Exception {
        var result = mockMvc.perform(get("/api/v1/stats/top-brands")
                        .param("type", "CAR")
                        .param("order", "asc")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();
        List<Double> prices = extractAvgPrices(result.getResponse().getContentAsString());
        assertThat(prices).isSortedAccordingTo(Double::compare);
    }

    @Test
    void topBrandsRespectsLimit() throws Exception {
        mockMvc.perform(get("/api/v1/stats/top-brands").param("type", "CAR").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)));
    }

    @Test
    void topBrandsWithInvalidOrderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stats/top-brands").param("type", "CAR").param("order", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topBrandsUsesLatestYearPerModelNotFullPriceHistory() {
        // BRM (2 modelos CAR, buggies clássicos com décadas de histórico) é o caso mais nítido
        // da distorção investigada: a média sobre TODOS os price_entries fica em ~R$39.158,33
        // (puxada pra baixo por preços muito antigos e depreciados); usando só o ano mais
        // recente de cada modelo, ~R$77.494,00 — quase o dobro. Os dois valores foram conferidos
        // à mão contra o banco de dev antes de escrever este teste.
        List<BrandAveragePriceProjection> projections = priceEntryRepository.findTopBrandsByAvgPriceDesc("CAR", 200);

        BrandAveragePriceProjection brm = projections.stream()
                .filter(p -> "BRM".equals(p.getBrandName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: marca BRM não encontrada"));

        assertThat(brm.getAvgPrice()).isEqualByComparingTo(new BigDecimal("77494.00"));
        assertThat(brm.getModelCount()).isEqualTo(2);
    }

    @Test
    void topBrandsModelCountIsNotInflatedByMultipleFuelsAtLatestYear() {
        // Fiat tem modelos reais com mais de um combustível no ano mais recente (ex.: "Elba
        // Weekend 1.5 i.e. 2p e 4p", id 1396: Gasolina + Álcool em 1996 — mesmo modelo usado nos
        // testes de /models/{id}/prices). Se cada combustível contasse como uma linha separada
        // em vez de ter a média tirada entre eles primeiro, modelCount apareceria inflado além
        // da contagem real de modelos da marca. O valor esperado é exatamente a contagem de
        // modelos distintos.
        Brand fiat = brandRepository.findByName("Fiat")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: marca Fiat não encontrada"));
        long expectedDistinctFiatCarModels =
                vehicleModelRepository.findByBrand_IdAndVehicleTypeOrderByNameAsc(fiat.getId(), "CAR").size();

        List<BrandAveragePriceProjection> projections = priceEntryRepository.findTopBrandsByAvgPriceDesc("CAR", 200);
        BrandAveragePriceProjection fiatStats = projections.stream()
                .filter(p -> "Fiat".equals(p.getBrandName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fiat deveria aparecer no ranking de CAR"));

        assertThat(fiatStats.getModelCount()).isEqualTo(expectedDistinctFiatCarModels);
    }

    @Test
    void fuelDistributionReturnsCountsPerFuelForCars() throws Exception {
        mockMvc.perform(get("/api/v1/stats/fuel-distribution").param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fuel", everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].count", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$[0].fuel", is("Gasolina")));
    }

    @Test
    void fuelDistributionWithoutTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stats/fuel-distribution"))
                .andExpect(status().isBadRequest());
    }

    private static List<Double> extractAvgPrices(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
        return root.findValues("avgPrice").stream().map(com.fasterxml.jackson.databind.JsonNode::asDouble).toList();
    }
}
