package com.fipeexplorer.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.RefreshTokenRepository;
import com.fipeexplorer.backend.repository.UserRepository;
import com.fipeexplorer.backend.repository.WatchedVehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev — mesma convenção dos demais testes de integração. Acura Integra
 * GS 1.8 (fipeCode "038003-2") é a mesma fixture conhecida usada em outros testes deste projeto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class WatchedVehicleControllerIntegrationTest {

    private static final String EMAIL_DOMAIN = "@watched-vehicle-integration-test.example.com";
    private static final String KNOWN_FIPE_CODE = "038003-2"; // Acura Integra GS 1.8

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private WatchedVehicleRepository watchedVehicleRepository;

    @AfterEach
    void cleanUp() {
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(EMAIL_DOMAIN))
                .toList();

        for (User user : testUsers) {
            watchedVehicleRepository.findAll().stream()
                    .filter(w -> w.getUser().getId().equals(user.getId()))
                    .forEach(watchedVehicleRepository::delete);
            refreshTokenRepository.deleteByUser(user);
            userRepository.delete(user);
        }
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
    void watchedVehiclesEndpointsRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/me/watched-vehicles")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s"}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void watchWithoutThresholdUsesDefaultFivePercent() throws Exception {
        String token = registerAndGetToken("ana" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s"}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fipeCode", is(KNOWN_FIPE_CODE)))
                .andExpect(jsonPath("$.brand", is("Acura")))
                .andExpect(jsonPath("$.thresholdPercent", is(0.05)));
    }

    @Test
    void watchingTheSameVehicleAgainUpdatesTheThreshold() throws Exception {
        String token = registerAndGetToken("bia" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s", "thresholdPercent": 0.05}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s", "thresholdPercent": 0.20}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdPercent", is(0.20)));

        mockMvc.perform(get("/api/v1/me/watched-vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // não duplicou, só atualizou
                .andExpect(jsonPath("$[0].thresholdPercent", is(0.20)));
    }

    @Test
    void watchingAnUnknownFipeCodeReturnsNotFound() throws Exception {
        String token = registerAndGetToken("carla" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "000000-0"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOnlyReturnsCurrentUsersWatchedVehicles() throws Exception {
        String tokenA = registerAndGetToken("diana" + EMAIL_DOMAIN);
        String tokenB = registerAndGetToken("elis" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s"}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/watched-vehicles").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/me/watched-vehicles").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unwatchRemovesItAndIsIdempotent() throws Exception {
        String token = registerAndGetToken("fabia" + EMAIL_DOMAIN);

        mockMvc.perform(post("/api/v1/me/watched-vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"fipeCode": "%s"}
                                """.formatted(KNOWN_FIPE_CODE)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/me/watched-vehicles/{fipeCode}", KNOWN_FIPE_CODE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/watched-vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Remover de novo (já removido) não é erro.
        mockMvc.perform(delete("/api/v1/me/watched-vehicles/{fipeCode}", KNOWN_FIPE_CODE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
