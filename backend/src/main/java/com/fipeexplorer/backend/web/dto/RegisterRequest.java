package com.fipeexplorer.backend.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "A senha precisa ter pelo menos 8 caracteres, com letras e números")
        String password,

        // boolean primitivo (não Boolean): campo ausente no JSON vira "false", não null - @AssertTrue
        // sozinho considera null válido, então usar o wrapper deixaria passar um cadastro sem o campo.
        @AssertTrue(message = "É preciso aceitar a política de privacidade para se cadastrar")
        boolean acceptedPrivacyPolicy
) {
}
