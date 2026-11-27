package org.dpp.backend.auth;

import org.dpp.backend.user.Role;

public record CurrentUserResponseDto(
        String username,
        Role role
) {
}
