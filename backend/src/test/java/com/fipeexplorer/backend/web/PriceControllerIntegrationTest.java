package com.fipeexplorer.backend.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
class PriceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pricesForAcuraIntegraIncludeBrandModelAndFipeCode() throws Exception {
        // Acura Integra GS 1.8 (fixture conhecida, mesmo modelo usado nos testes de catálogo).
        mockMvc.perform(get("/api/v1/models/{modelId}/prices", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId", is(1)))
                .andExpect(jsonPath("$.brand", is("Acura")))
                .andExpect(jsonPath("$.model", is("Integra GS 1.8")))
                .andExpect(jsonPath("$.fipeCode", is("038003-2")))
                .andExpect(jsonPath("$.prices", not(org.hamcrest.Matchers.empty())));
    }

    @Test
    void pricesForUnknownModelReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/models/{modelId}/prices", 999_999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void modelWithMultipleFuelsAcrossOverlappingYearsExposesBothInPriceList() throws Exception {
        // Fiat Elba Weekend 1.5 i.e. 2p e 4p: Gasolina e Álcool coexistem em 1995 e 1996
        // (year_code "1995-1"/"1995-2" etc.) — caso real usado pra validar múltiplas séries
        // no gráfico de depreciação do frontend.
        mockMvc.perform(get("/api/v1/models/{modelId}/prices", 1396))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model", is("Elba Weekend 1.5 i.e. 2p e 4p")))
                .andExpect(jsonPath("$.prices[*].fuel", hasItem("Gasolina")))
                .andExpect(jsonPath("$.prices[*].fuel", hasItem("Álcool")))
                .andExpect(jsonPath("$.prices[*].price", everyItem(org.hamcrest.Matchers.notNullValue())));
    }
}
