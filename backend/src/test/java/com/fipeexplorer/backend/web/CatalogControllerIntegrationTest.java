package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.repository.BrandRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
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
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void vehicleTypesReturnsTheThreeKnownTypes() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder("CAR", "MOTORCYCLE", "TRUCK")));
    }

    @Test
    void brandsFilteredByCarIncludesAcura() throws Exception {
        mockMvc.perform(get("/api/v1/brands").param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Acura")));
    }

    @Test
    void brandsWithoutTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modelsForAcuraCarIncludesIntegra() throws Exception {
        Brand acura = brandRepository.findByName("Acura")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: marca Acura não encontrada no banco de dev"));

        mockMvc.perform(get("/api/v1/brands/{brandId}/models", acura.getId()).param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Integra GS 1.8")));
    }

    @Test
    void modelsForUnknownBrandReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/brands/{brandId}/models", 999_999_999L).param("type", "CAR"))
                .andExpect(status().isNotFound());
    }

    /**
     * A FIPE cataloga "BMW" como marcas distintas por tipo de veículo — fipe_code 7 (carros) e
     * fipe_code 67 (motos) são {@code brand} rows diferentes, cada uma com um único
     * vehicle_type. Não existe, nos dados reais, um único brand_id com modelos de mais de um
     * tipo. Esses testes comprovam que o filtro em /brands é feito via join/exists contra
     * vehicle_model (não um campo fixo em brand): a marca "BMW carros" aparece só em type=CAR e
     * a "BMW motos" só em type=MOTORCYCLE, mesmo as duas compartilhando o nome "BMW".
     */
    @Test
    void carBmwAppearsOnlyUnderCarTypeNotMotorcycle() throws Exception {
        Brand bmwCar = brandRepository.findByFipeCode("7")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: BMW carros (fipe_code 7) não encontrada"));

        mockMvc.perform(get("/api/v1/brands").param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bmwCar.getId().intValue())));

        mockMvc.perform(get("/api/v1/brands").param("type", "MOTORCYCLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(bmwCar.getId().intValue()))));
    }

    @Test
    void motorcycleBmwAppearsOnlyUnderMotorcycleTypeNotCar() throws Exception {
        Brand bmwMotorcycle = brandRepository.findByFipeCode("67")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: BMW motos (fipe_code 67) não encontrada"));

        mockMvc.perform(get("/api/v1/brands").param("type", "MOTORCYCLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bmwMotorcycle.getId().intValue())));

        mockMvc.perform(get("/api/v1/brands").param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(bmwMotorcycle.getId().intValue()))));
    }

    @Test
    void modelsEndpointFiltersByTypeEvenWhenBrandExists() throws Exception {
        Brand bmwCar = brandRepository.findByFipeCode("7")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: BMW carros (fipe_code 7) não encontrada"));

        mockMvc.perform(get("/api/v1/brands/{brandId}/models", bmwCar.getId()).param("type", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        // BMW carros (id do fipe_code 7) não tem nenhum modelo do tipo MOTORCYCLE: marca existe
        // (200, não 404), mas a lista de modelos vem vazia.
        mockMvc.perform(get("/api/v1/brands/{brandId}/models", bmwCar.getId()).param("type", "MOTORCYCLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}
