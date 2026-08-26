package com.fipeexplorer.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipeexplorer.backend.domain.User;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bate direto no Postgres de dev (docker-compose, localhost:5433) — mesma convenção dos demais
 * testes de integração deste projeto. Cada teste usa um e-mail próprio (não reaproveita fixtures
 * do CSV) e limpa suas próprias linhas em @AfterEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("integration")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void cleanUp() {
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@auth-integration-test.example.com"))
                .forEach(u -> {
                    refreshTokenRepository.deleteByUser(u);
                    userRepository.delete(u);
                });
    }

    private String registerBody(String email, String password) throws Exception {
        return """
                {"email": "%s", "password": "%s", "acceptedPrivacyPolicy": true}
                """.formatted(email, password);
    }

    private JsonNode registerAndParse(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody(email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void registerCreatesUserAndReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody("ana@auth-integration-test.example.com", "senha1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())));
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        registerAndParse("bia@auth-integration-test.example.com", "senha1234");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody("bia@auth-integration-test.example.com", "outrasenha123")))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithWeakPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerBody("carla@auth-integration-test.example.com", "123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithoutAcceptingPrivacyPolicyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email": "ines@auth-integration-test.example.com", "password": "senha1234", "acceptedPrivacyPolicy": false}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("ines@auth-integration-test.example.com")).isEmpty();
    }

    @Test
    void registerRecordsPrivacyAcceptedAtAtTheMomentOfRegistration() throws Exception {
        Instant before = Instant.now();

        registerAndParse("joana@auth-integration-test.example.com", "senha1234");

        Instant after = Instant.now();

        User user = userRepository.findByEmail("joana@auth-integration-test.example.com")
                .orElseThrow(() -> new IllegalStateException("Usuário recém-cadastrado não encontrado"));

        assertThat(user.getPrivacyAcceptedAt()).isBetween(before, after);
    }

    @Test
    void loginWithCorrectCredentialsReturnsTokens() throws Exception {
        registerAndParse("diana@auth-integration-test.example.com", "senha1234");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(registerBody("diana@auth-integration-test.example.com", "senha1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorizedWithGenericMessage() throws Exception {
        registerAndParse("elis@auth-integration-test.example.com", "senha1234");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(registerBody("elis@auth-integration-test.example.com", "senhaerrada")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String wrongPasswordMessage = result.getResponse().getErrorMessage();

        MvcResult unknownEmailResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(registerBody("ninguem@auth-integration-test.example.com", "qualquersenha")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // e-mail inexistente e senha errada devem devolver exatamente a mesma mensagem -
        // ninguém consegue descobrir se um e-mail está cadastrado só tentando logar com ele.
        assertThat(unknownEmailResult.getResponse().getErrorMessage()).isEqualTo(wrongPasswordMessage);
    }

    @Test
    void refreshWithValidTokenReturnsNewAccessToken() throws Exception {
        JsonNode tokens = registerAndParse("fabia@auth-integration-test.example.com", "senha1234");
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", is(refreshToken)));
    }

    @Test
    void refreshWithUnknownTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken": "token-que-nunca-existiu"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenSoSubsequentRefreshFails() throws Exception {
        JsonNode tokens = registerAndParse("gabi@auth-integration-test.example.com", "senha1234");
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithInvalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer isto-nao-e-um-jwt-valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsEmail() throws Exception {
        JsonNode tokens = registerAndParse("helena@auth-integration-test.example.com", "senha1234");
        String accessToken = tokens.get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("helena@auth-integration-test.example.com")));
    }

    @Test
    void existingPublicEndpointsStillWorkWithoutAnyToken() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-types"))
                .andExpect(status().isOk());
    }
}
