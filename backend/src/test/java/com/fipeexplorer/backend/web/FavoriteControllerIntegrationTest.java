package com.fipeexplorer.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.FavoriteVehicleRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.repository.RefreshTokenRepository;
import com.fipeexplorer.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class FavoriteControllerIntegrationTest {

    private static final String EMAIL_DOMAIN = "@favorites-integration-test.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FavoriteVehicleRepository favoriteVehicleRepository;

    @Autowired
    private PriceEntryRepository priceEntryRepository;

    @AfterEach
    void cleanUp() {
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(EMAIL_DOMAIN))
                .toList();

        for (User user : testUsers) {
            favoriteVehicleRepository.findAll().stream()
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .forEach(favoriteVehicleRepository::delete);
            refreshTokenRepository.deleteByUser(user);
            userRepository.delete(user);
        }
    }

    private Long knownPriceEntryId() {
        // Acura Integra GS 1.8 (modelId 1, fixture conhecida, mesma usada em outros testes).
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

    private String favoriteBody(Long priceEntryId) {
        return """
                {"priceEntryId": %d}
                """.formatted(priceEntryId);
    }

    @Test
    void favoritesEndpointsRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/me/favorites")
                        .contentType("application/json")
                        .content(favoriteBody(1L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addFavoriteCreatesAndReturnsTheVehicle() throws Exception {
        String token = registerAndGetToken("ana" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(favoriteBody(priceEntryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(priceEntryId.intValue())))
                .andExpect(jsonPath("$.brand", is("Acura")))
                .andExpect(jsonPath("$.model", is("Integra GS 1.8")));
    }

    @Test
    void addingTheSameFavoriteTwiceIsIdempotentAndDoesNotDuplicate() throws Exception {
        String token = registerAndGetToken("bia" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(favoriteBody(priceEntryId)))
                .andExpect(status().isCreated());

        // Segunda vez: mesmo par (usuário, priceEntry) - não é erro, só devolve o que já existia.
        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(favoriteBody(priceEntryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(priceEntryId.intValue())));

        mockMvc.perform(get("/api/v1/me/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void addFavoriteForUnknownPriceEntryReturnsNotFound() throws Exception {
        String token = registerAndGetToken("carla" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(favoriteBody(999_999_999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFavoritesOnlyReturnsCurrentUsersFavorites() throws Exception {
        String tokenA = registerAndGetToken("diana" + EMAIL_DOMAIN);
        String tokenB = registerAndGetToken("elis" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content(favoriteBody(priceEntryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/favorites").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/me/favorites").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void removeFavoriteDeletesItFromTheList() throws Exception {
        String token = registerAndGetToken("fabia" + EMAIL_DOMAIN);
        Long priceEntryId = knownPriceEntryId();

        mockMvc.perform(post("/api/v1/me/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(favoriteBody(priceEntryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/me/favorites/{priceEntryId}", priceEntryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/favorites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void removingAFavoriteThatWasNeverAddedIsIdempotent() throws Exception {
        String token = registerAndGetToken("gabi" + EMAIL_DOMAIN);

        mockMvc.perform(delete("/api/v1/me/favorites/{priceEntryId}", knownPriceEntryId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void existingPublicEndpointsStillWorkWithoutAnyToken() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-types")).andExpect(status().isOk());
    }
}
