package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev (docker-compose, localhost:5433) já populado pelo import real
 * do CSV da Tabela FIPE — mesma convenção dos demais testes de integração deste projeto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class PriceEstimateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    private Long knownPriceEntryId() {
        // Acura Integra GS 1.8 (modelId 1, fixture conhecida, mesma usada em PriceControllerIntegrationTest).
        List<PriceEntry> entries = priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(1L);
        if (entries.isEmpty()) {
            throw new IllegalStateException(
                    "Fixture esperada ausente: nenhum price_entry para o modelo 1 (Acura Integra GS 1.8)");
        }
        return entries.get(0).getId();
    }

    @Test
    void estimateForKnownPriceEntryReturnsBaseAndAdjustedPriceWithAllComponents() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", knownPriceEntryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": 50000, "condition": "BOM", "extras": ["AR_CONDICIONADO", "TETO_SOLAR"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice", notNullValue()))
                .andExpect(jsonPath("$.adjustedPrice", notNullValue()))
                .andExpect(jsonPath("$.components", hasSize(4)))
                .andExpect(jsonPath("$.components[*].key",
                        hasItems("MILEAGE", "CONDITION", "EXTRA:AR_CONDICIONADO", "EXTRA:TETO_SOLAR")));
    }

    @Test
    void estimateForUnknownPriceEntryReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", 999_999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": 50000, "condition": "BOM", "extras": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void estimateWithInvalidConditionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", knownPriceEntryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": 50000, "condition": "PERFEITO", "extras": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estimateWithUnknownExtraReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", knownPriceEntryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": 50000, "condition": "BOM", "extras": ["JATO_TURBO"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estimateWithNegativeKmReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", knownPriceEntryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": -100, "condition": "BOM", "extras": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estimateWithMissingConditionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles/{priceEntryId}/price-estimate", knownPriceEntryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"km": 50000, "extras": []}
                                """))
                .andExpect(status().isBadRequest());
    }
}
