package org.dpp.backend.repository;

import org.dpp.backend.user.Role;
import org.dpp.backend.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    boolean existsByRole(Role role);
}
