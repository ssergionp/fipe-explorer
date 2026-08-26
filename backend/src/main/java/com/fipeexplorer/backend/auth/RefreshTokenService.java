package com.fipeexplorer.backend.auth;

import com.fipeexplorer.backend.domain.RefreshToken;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token é uma string opaca (UUID aleatório), não um JWT — só o access token é assinado.
 * Fica guardado no banco pra poder ser revogado (logout) e pra existir no máximo uma sessão ativa
 * por usuário: um novo login apaga qualquer refresh token anterior daquele usuário.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = Duration.ofDays(properties.getRefreshTokenExpirationDays());
    }

    @Transactional
    public RefreshToken create(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(), user, Instant.now().plus(refreshTokenExpiration));
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }
}
