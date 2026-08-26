package com.fipeexplorer.backend.auth;

import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolve o User autenticado a partir do Authentication - reaproveitado pelos controllers de /me/**. */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado não encontrado: " + authentication.getName()));
    }
}
