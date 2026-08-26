package com.fipeexplorer.backend.auth;

import com.fipeexplorer.backend.domain.RefreshToken;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.UserRepository;
import com.fipeexplorer.backend.web.dto.AuthResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "E-mail ou senha inválidos.";
    private static final String EMAIL_ALREADY_REGISTERED_MESSAGE = "E-mail já cadastrado.";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token inválido ou expirado.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService,
                        JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_ALREADY_REGISTERED_MESSAGE);
        }

        User user = new User(email, passwordEncoder.encode(rawPassword));
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // corrida rara entre o existsByEmail acima e o insert - a constraint UNIQUE do banco
            // é a garantia de verdade, isso aqui só traduz pra um 409 em vez de vazar um 500.
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_ALREADY_REGISTERED_MESSAGE);
        }

        return issueTokens(user);
    }

    public AuthResponse login(String email, String rawPassword) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (AuthenticationException e) {
            // mesma mensagem genérica pra "e-mail não existe" e "senha errada" - não dá pra
            // descobrir se um e-mail está cadastrado só tentando logar com ele.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE));

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenValue)
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN_MESSAGE));

        UserDetails userDetails = userDetailsService.loadUserByUsername(refreshToken.getUser().getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        // sem rotação: o mesmo refresh token continua valendo até expirar ou ser revogado no logout.
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        // idempotente por design: token desconhecido/já revogado não é erro, só não tem o que revogar.
        refreshTokenService.findByToken(refreshTokenValue).ifPresent(refreshTokenService::revoke);
    }

    private AuthResponse issueTokens(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }
}
