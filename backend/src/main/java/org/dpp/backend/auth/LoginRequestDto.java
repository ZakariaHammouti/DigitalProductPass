package org.dpp.backend.auth;

public record LoginRequestDto(
        String username,
        String password
) {
}
