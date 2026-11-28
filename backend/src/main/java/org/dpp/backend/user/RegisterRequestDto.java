package org.dpp.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.dpp.backend.help.MaxBytes;

public record RegisterRequestDto(
        @NotBlank(message = "Benutzername darf nicht leer sein")
        String username,
        @NotBlank(message = "E-Mail darf nicht leer sein")
        String email,

        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Passwort braucht mindestens 8 Zeichen, " +
                        "einen Groß- und einen Kleinbuchstaben," +
                        " eine Ziffer und " +
                        "ein Sonderzeichen")
        @MaxBytes(value = 72, message = "Passwort ist zu lang")
        @NotBlank(message = "Passwort darf nicht leer sein")
        String password,
        @NotBlank(message = "Firma darf nicht leer sein")
        String company,
        @NotNull(message = "Rolle darf nicht leer sein")
        Role role
) {
}

