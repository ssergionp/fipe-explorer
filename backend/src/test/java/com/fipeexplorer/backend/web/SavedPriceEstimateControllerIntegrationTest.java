package com.fipeexplorer.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.RefreshTokenRepository;
import com.fipeexplorer.backend.repository.SavedPriceEstimateRepository;
import com.fipeexplorer.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev (docker-compose, localhost:5433) — mesma convenção dos demais
 * testes de integração deste projeto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class SavedPriceEstimateControllerIntegrationTest {

    private static final String EMAIL_DOMAIN = "@estimates-integration-test.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SavedPriceEstimateRepository savedPriceEstimateRepository;

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @AfterEach
    void cleanUp() {
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(EMAIL_DOMAIN))
                .toList();

        for (User user : testUsers) {
            savedPriceEstimateRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .forEach(savedPriceEstimateRepository::delete);
            refreshTokenRepository.deleteByUser(user);
            userRepository.delete(user);
        }
    }

    private Long knownPriceEntryId() {
        List<PriceEntry> entries = priceEntryRepository.findByVehicleModel_IdOrderByYearCodeAsc(1L);
        if (entries.isEmpty()) {
            throw new IllegalStateException("Fixture esperada ausente: price_entry do modelo 1 não encontrado");
        }
        return entries.get(0).getId();
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email": "%s", "password": "senha1234", "acceptedPrivacyPolicy": true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void priceEstimatesEndpointsRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/me/price-estimates")).andExpect(status().isUnauthorized());
    }

    @Test
    void saveCreatesEstimateWithServerRecalculatedComponents() throws Exception {
        String token = registerAndGetToken("helena" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": %d, "km": 50000, "condition": "BOM", "extras": ["AR_CONDICIONADO"]}
                                """.formatted(priceEntryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(0)))
                .andExpect(jsonPath("$.vehicle.brand", is("Acura")))
                .andExpect(jsonPath("$.km", is(50000)))
                .andExpect(jsonPath("$.condition", is("BOM")))
                .andExpect(jsonPath("$.extras", hasSize(1)))
                .andExpect(jsonPath("$.components", hasSize(3))); // km + condição + 1 opcional
    }

    /**
     * O request nunca tem campo adjustedPrice (o DTO nem declara um), mas simulamos um cliente
     * malicioso mandando um valor forjado dentro do JSON mesmo assim — Jackson ignora campos
     * desconhecidos, então o servidor nunca chega a ler esse valor. Provamos isso checando que
     * (a) o valor forjado não aparece na resposta e (b) adjustedPrice bate exatamente com
     * basePrice + soma dos componentes que o próprio servidor devolveu — só é possível se o
     * cálculo foi feito do zero no servidor, não copiado do cliente.
     */
    @Test
    void saveIgnoresAnyClientSuppliedAdjustedPriceAndRecalculatesServerSide() throws Exception {
        String token = registerAndGetToken("ines" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        MvcResult result = mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": %d, "km": 30000, "condition": "EXCELENTE", "extras": [], "adjustedPrice": "999999.00"}
                                """.formatted(priceEntryId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal basePrice = new BigDecimal(body.get("basePrice").asText());
        BigDecimal adjustedPrice = new BigDecimal(body.get("adjustedPrice").asText());

        assertThat(adjustedPrice).isNotEqualByComparingTo("999999.00");

        BigDecimal sumOfComponents = BigDecimal.ZERO;
        for (JsonNode component : body.get("components")) {
            sumOfComponents = sumOfComponents.add(new BigDecimal(component.get("amount").asText()));
        }
        assertThat(adjustedPrice).isEqualByComparingTo(basePrice.add(sumOfComponents));
    }

    @Test
    void saveForUnknownPriceEntryReturnsNotFound() throws Exception {
        String token = registerAndGetToken("joana" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": 999999999, "km": 1000, "condition": "BOM", "extras": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsOnlyCurrentUsersSavedEstimates() throws Exception {
        String tokenA = registerAndGetToken("karen" + EMAIL_DOMAIN);
        String tokenB = registerAndGetToken("luiza" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": %d, "km": 1000, "condition": "BOM", "extras": []}
                                """.formatted(priceEntryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/price-estimates").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/me/price-estimates").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteRemovesSavedEstimate() throws Exception {
        String token = registerAndGetToken("monica" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        MvcResult result = mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": %d, "km": 1000, "condition": "BOM", "extras": []}
                                """.formatted(priceEntryId)))
                .andExpect(status().isCreated())
                .andReturn();
        long savedId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/me/price-estimates/{id}", savedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/price-estimates").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteNonexistentSavedEstimateReturnsNotFound() throws Exception {
        String token = registerAndGetToken("nadia" + EMAIL_DOMAIN);

        mockMvc.perform(delete("/api/v1/me/price-estimates/{id}", 999_999_999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAnotherUsersSavedEstimateReturnsNotFound() throws Exception {
        String tokenA = registerAndGetToken("olga" + EMAIL_DOMAIN);
        String tokenB = registerAndGetToken("paula" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        MvcResult result = mockMvc.perform(post("/api/v1/me/price-estimates")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("""
                                {"priceEntryId": %d, "km": 1000, "condition": "BOM", "extras": []}
                                """.formatted(priceEntryId)))
                .andExpect(status().isCreated())
                .andReturn();
        long savedId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/me/price-estimates/{id}", savedId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
