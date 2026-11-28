package org.dpp.backend.user;

import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Document("users") //sonst nimmt spring user also klassenname kleingeschrieben
public record AppUser(
        String id,
        String username,
        String password,
        Role role,
        AccountStatus accountStatus,
        String email,
        String company
) {
}
