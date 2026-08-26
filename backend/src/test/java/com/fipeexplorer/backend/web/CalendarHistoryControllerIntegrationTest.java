package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.external.FipeExternalApiProperties;
import com.fipeexplorer.backend.repository.ExternalPriceHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate no Postgres de dev (docker-compose, localhost:5433) pra exercitar o cache real, mas NUNCA
 * chama a API pública da FIPE de verdade — o bean RestClient é substituído por um
 * MockRestServiceServer (ver {@link MockExternalApiConfig}), então não consome a cota diária
 * (500/dia sem token) nem depende de rede.
 *
 * <p>O corpo JSON usado para o caso "sucesso" foi copiado literalmente de uma chamada real feita
 * durante o planejamento desta feature (GET .../cars/001025-1/years/1996-1/history, Fiat Elba,
 * fipeCode 001025-1) — confirma que o formato de year_code do nosso banco ("1996-1") e de
 * fipe_price_code ("001025-1") batem com o yearId/fipeCode esperados pela API v2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class CalendarHistoryControllerIntegrationTest {

    private static final String REAL_ELBA_HISTORY_RESPONSE = """
            {
              "vehicleType": 1,
              "brand": "Fiat",
              "model": "Elba 1.6i.e/Top/CSL/ 1.6i.e/1.5 2p e 4p",
              "modelYear": 1996,
              "fuel": "Gasolina",
              "codeFipe": "001025-1",
              "fuelAcronym": "G",
              "priceHistory": [
                {"price": "R$ 13.017,00", "month": "agosto de 2026", "reference": "336"},
                {"price": "R$ 12.889,00", "month": "julho de 2026", "reference": "335"},
                {"price": "R$ 12.762,00", "month": "junho de 2026", "reference": "334"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FipeExternalApiProperties properties;

    @Autowired
    private ExternalPriceHistoryRepository externalPriceHistoryRepository;

    @Autowired
    private MockExternalApiConfig.MockServerHolder mockServerHolder;

    @BeforeEach
    void cleanUpCacheFixtures() {
        deleteCacheRowIfPresent("CAR", "001025-1", "1996-1");
        deleteCacheRowIfPresent("CAR", "000000-0", "1999-1");
        deleteCacheRowIfPresent("CAR", "111111-1", "2000-1");
        deleteCacheRowIfPresent("CAR", "222222-2", "2001-1");
    }

    @AfterEach
    void verifyAndResetMockServer() {
        mockServerHolder.server.verify();
        mockServerHolder.server.reset();
    }

    private void deleteCacheRowIfPresent(String vehicleType, String fipeCode, String yearCode) {
        externalPriceHistoryRepository.findByVehicleTypeAndFipeCodeAndYearCode(vehicleType, fipeCode, yearCode)
                .ifPresent(externalPriceHistoryRepository::delete);
    }

    private String externalUri(String path) {
        return properties.getBaseUrl() + path;
    }

    @Test
    void availableHistoryIsFetchedFromExternalApiAndThenServedFromCache() throws Exception {
        mockServerHolder.server.expect(requestTo(externalUri("/cars/001025-1/years/1996-1/history")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(REAL_ELBA_HISTORY_RESPONSE, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/v1/vehicles/calendar-history")
                        .param("type", "CAR")
                        .param("fipeCode", "001025-1")
                        .param("yearCode", "1996-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("AVAILABLE")))
                .andExpect(jsonPath("$.cached", org.hamcrest.Matchers.is(false)))
                .andExpect(jsonPath("$.months", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.months[0].price", org.hamcrest.Matchers.is(12762.00)))
                .andExpect(jsonPath("$.months[2].price", org.hamcrest.Matchers.is(13017.00)));

        assertThat(externalPriceHistoryRepository.findByVehicleTypeAndFipeCodeAndYearCode(
                "CAR", "001025-1", "1996-1")).isPresent();

        // Segunda chamada não deve bater na API externa de novo (só uma expectativa foi
        // registrada acima) — vem inteiramente do cache.
        mockMvc.perform(get("/api/v1/vehicles/calendar-history")
                        .param("type", "CAR")
                        .param("fipeCode", "001025-1")
                        .param("yearCode", "1996-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("AVAILABLE")))
                .andExpect(jsonPath("$.cached", org.hamcrest.Matchers.is(true)))
                .andExpect(jsonPath("$.months", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    void notFoundFromExternalApiReturnsHttp200WithNotFoundStatus() throws Exception {
        mockServerHolder.server.expect(requestTo(externalUri("/cars/000000-0/years/1999-1/history")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"veículo não encontrado para a referência informada\"}"));

        mockMvc.perform(get("/api/v1/vehicles/calendar-history")
                        .param("type", "CAR")
                        .param("fipeCode", "000000-0")
                        .param("yearCode", "1999-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("NOT_FOUND")))
                .andExpect(jsonPath("$.reason", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.months", org.hamcrest.Matchers.empty()));

        assertThat(externalPriceHistoryRepository.findByVehicleTypeAndFipeCodeAndYearCode(
                "CAR", "000000-0", "1999-1")).isPresent();
    }

    @Test
    void rateLimitedFromExternalApiReturnsHttp200WithRateLimitedStatusAndIsNotCached() throws Exception {
        mockServerHolder.server.expect(requestTo(externalUri("/cars/111111-1/years/2000-1/history")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(get("/api/v1/vehicles/calendar-history")
                        .param("type", "CAR")
                        .param("fipeCode", "111111-1")
                        .param("yearCode", "2000-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("RATE_LIMITED")))
                .andExpect(jsonPath("$.reason", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.months", org.hamcrest.Matchers.empty()));

        assertThat(externalPriceHistoryRepository.findByVehicleTypeAndFipeCodeAndYearCode(
                "CAR", "111111-1", "2000-1")).isEmpty();
    }

    @Test
    void networkFailureReturnsHttp200WithUnavailableStatusAndIsNotCached() throws Exception {
        mockServerHolder.server.expect(requestTo(externalUri("/cars/222222-2/years/2001-1/history")))
                .andRespond(request -> {
                    throw new IOException("connection reset (simulado)");
                });

        mockMvc.perform(get("/api/v1/vehicles/calendar-history")
                        .param("type", "CAR")
                        .param("fipeCode", "222222-2")
                        .param("yearCode", "2001-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("UNAVAILABLE")))
                .andExpect(jsonPath("$.reason", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.months", org.hamcrest.Matchers.empty()));

        assertThat(externalPriceHistoryRepository.findByVehicleTypeAndFipeCodeAndYearCode(
                "CAR", "222222-2", "2001-1")).isEmpty();
    }

    @TestConfiguration
    static class MockExternalApiConfig {

        static class MockServerHolder {
            MockRestServiceServer server;
        }

        @Bean
        MockServerHolder mockServerHolder() {
            return new MockServerHolder();
        }

        @Bean
        @Primary
        RestClient mockedFipeExternalApiRestClient(FipeExternalApiProperties properties, MockServerHolder holder) {
            RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
            holder.server = MockRestServiceServer.bindTo(builder).build();
            return builder.build();
        }
    }
}
