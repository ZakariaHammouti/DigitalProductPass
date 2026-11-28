package org.dpp.backend.repository;

import org.dpp.backend.user.Role;
import org.dpp.backend.user.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<AppUser, String> {

    boolean existsByRole(Role role);

    Optional<AppUser> findByUsername(String username);
}
