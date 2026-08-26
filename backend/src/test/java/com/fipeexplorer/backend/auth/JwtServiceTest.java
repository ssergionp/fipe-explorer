package com.fipeexplorer.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "xuuMCZakUvLTz71IR+pD0X4wKOaukd5vZt9Ar21FIWhXv+ZP9X7OUcHWxh92XtBteDHS1mgg7T2gUhxQ3vcwZQ==";

    private JwtService serviceWithExpirationMinutes(long minutes) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpirationMinutes(minutes);
        properties.setRefreshTokenExpirationDays(7);
        return new JwtService(properties);
    }

    private UserDetails userDetails(String email) {
        return User.withUsername(email).password("irrelevant").authorities(List.of()).build();
    }

    @Test
    void generatedTokenCarriesEmailAsSubject() {
        JwtService jwtService = serviceWithExpirationMinutes(15);
        UserDetails user = userDetails("ana@example.com");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("ana@example.com");
    }

    @Test
    void tokenIsValidForTheUserItWasIssuedTo() {
        JwtService jwtService = serviceWithExpirationMinutes(15);
        UserDetails user = userDetails("ana@example.com");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isValid(token, user)).isTrue();
    }

    @Test
    void tokenIsInvalidForADifferentUser() {
        JwtService jwtService = serviceWithExpirationMinutes(15);
        String token = jwtService.generateAccessToken(userDetails("ana@example.com"));

        assertThat(jwtService.isValid(token, userDetails("outro@example.com"))).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() throws InterruptedException {
        // expiração em ~0ms - o token nasce expirado ou expira no instante seguinte.
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpirationMinutes(0);
        JwtService jwtService = new JwtService(properties);
        UserDetails user = userDetails("ana@example.com");

        String token = jwtService.generateAccessToken(user);
        Thread.sleep(50);

        assertThat(jwtService.isValid(token, user)).isFalse();
    }

    @Test
    void malformedTokenIsInvalid() {
        JwtService jwtService = serviceWithExpirationMinutes(15);
        UserDetails user = userDetails("ana@example.com");

        assertThat(jwtService.isValid("isto.não.é-um-jwt", user)).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretIsInvalid() {
        JwtProperties otherSecretProperties = new JwtProperties();
        otherSecretProperties.setSecret(Base64.getEncoder().encodeToString("outra-chave-completamente-diferente-de-32-bytes!!".getBytes()));
        otherSecretProperties.setAccessTokenExpirationMinutes(15);
        JwtService attacker = new JwtService(otherSecretProperties);

        JwtService realService = serviceWithExpirationMinutes(15);
        UserDetails user = userDetails("ana@example.com");

        String forgedToken = attacker.generateAccessToken(user);

        assertThat(realService.isValid(forgedToken, user)).isFalse();
    }
}
