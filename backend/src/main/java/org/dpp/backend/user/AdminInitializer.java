package org.dpp.backend.user;

import lombok.extern.slf4j.Slf4j;
import org.dpp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

//logger
@Slf4j
//zum starten beim start der app
@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username}") String adminUsername,
                            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean adminExists = userRepository.existsByRole(Role.ADMIN);

        if (!adminExists) {
            if (adminUsername.isBlank() || adminPassword.isBlank()) {
                log.warn("Admin-Benutzername oder Passwort ist nicht gesetzt. Bitte setzen Sie die Umgebungsvariablen ADMIN_USERNAME und ADMIN_PASSWORD.");
                return;
            }
            
            User adminUser = User.builder()
                    .id(UUID.randomUUID().toString())
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            userRepository.save(adminUser);
            log.info("Admin-Benutzer erstellt: {}", adminUsername);
        } else {
            log.info("Admin-Benutzer existiert bereits.");
        }
    }
}

