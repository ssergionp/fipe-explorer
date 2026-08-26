package com.fipeexplorer.backend.web.dto;

public record AuthResponse(
        String token,
        String refreshToken
) {
}
