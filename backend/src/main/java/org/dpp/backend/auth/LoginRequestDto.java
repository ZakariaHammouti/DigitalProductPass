package org.dpp.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "Benutzername darf nicht leer sein")
        String username,
        @NotBlank(message = "Passwort darf nicht leer sein")
        String password
) {
}
