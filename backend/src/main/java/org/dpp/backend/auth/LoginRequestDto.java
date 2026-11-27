package org.dpp.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.dpp.backend.help.MaxBytes;

public record LoginRequestDto(
        @NotBlank(message = "Benutzername darf nicht leer sein")
        @Size(max = 50, message = "Benutzername darf höchstens 50 Zeichen haben")
        String username,
        //BCrypt nutzt höchstens 72 Bytes
        @NotBlank(message = "Passwort darf nicht leer sein")
        @MaxBytes(value = 72, message = "Passwort ist zu lang")
        String password
) {
}
