package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.domain.Brand;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.repository.BrandRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
class VehicleSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @Test
    void combinesTypeBrandFuelAndPriceRangeFilters() throws Exception {
        Brand acura = brandRepository.findByName("Acura")
                .orElseThrow(() -> new IllegalStateException("Fixture esperada ausente: marca Acura não encontrada no banco de dev"));

        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("type", "CAR")
                        .param("brandId", acura.getId().toString())
                        .param("fuel", "Gasolina")
                        .param("minPrice", "0")
                        .param("maxPrice", "1000000")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", greaterThan(0)))
                .andExpect(jsonPath("$.items[*].brand", everyItem(is("Acura"))))
                .andExpect(jsonPath("$.items[*].fuel", everyItem(is("Gasolina"))));
    }

    @Test
    void secondPageReturnsDifferentItemsFromFirstPage() throws Exception {
        var page1 = mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.items.length()", is(20)))
                .andReturn().getResponse().getContentAsString();

        var page2 = mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.items.length()", is(20)))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(page1).isNotEqualTo(page2);
    }

    @Test
    void withoutAnyFilterReturnsEverythingPaginated() throws Exception {
        long totalPriceEntries = priceEntryRepository.count();

        mockMvc.perform(get("/api/v1/vehicles/search").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is((int) totalPriceEntries)))
                .andExpect(jsonPath("$.items.length()", is(10)))
                .andExpect(jsonPath("$.items[*].modelId", everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void pageSizeIsCappedAtMax() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", lessThanOrEqualTo(100)));
    }

    @Test
    void compareWithTwoValidIdsReturnsBothInRequestedOrder() throws Exception {
        List<Long> ids = priceEntryRepository.findAll(PageRequest.of(0, 2)).getContent().stream()
                .map(PriceEntry::getId)
                .toList();

        mockMvc.perform(get("/api/v1/vehicles/compare").param("ids", joinIds(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", contains(ids.get(0).intValue(), ids.get(1).intValue())));
    }

    @Test
    void compareWithFourValidIdsReturnsAllFour() throws Exception {
        List<Long> ids = priceEntryRepository.findAll(PageRequest.of(0, 4)).getContent().stream()
                .map(PriceEntry::getId)
                .toList();

        mockMvc.perform(get("/api/v1/vehicles/compare").param("ids", joinIds(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void compareWithFewerThanTwoIdsReturnsBadRequest() throws Exception {
        Long id = priceEntryRepository.findAll(PageRequest.of(0, 1)).getContent().get(0).getId();

        mockMvc.perform(get("/api/v1/vehicles/compare").param("ids", id.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compareWithMoreThanFourIdsReturnsBadRequest() throws Exception {
        List<Long> ids = priceEntryRepository.findAll(PageRequest.of(0, 5)).getContent().stream()
                .map(PriceEntry::getId)
                .toList();

        mockMvc.perform(get("/api/v1/vehicles/compare").param("ids", joinIds(ids)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compareIgnoresNonexistentIdMixedWithValidOnes() throws Exception {
        // Um id inexistente na lista não derruba a comparação inteira: ele é descartado
        // silenciosamente e só o(s) id(s) válido(s) aparecem na resposta (a contagem validada
        // é a de ids pedidos, não a de ids efetivamente encontrados).
        Long validId = priceEntryRepository.findAll(PageRequest.of(0, 1)).getContent().get(0).getId();
        long nonexistentId = 999_999_999L;

        mockMvc.perform(get("/api/v1/vehicles/compare").param("ids", validId + "," + nonexistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(validId.intValue())));
    }

    private static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
