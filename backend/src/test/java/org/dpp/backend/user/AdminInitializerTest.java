package org.dpp.backend.user;

import org.dpp.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminInitializerTest {
    private final UserRepository userRepository = mock(UserRepository.class);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void run_createsActiveAdminWithHashedPassword_whenNoAdminExists() {
        // given
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin", "geheim123");

        // when
        initializer.run(null);

        // then
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        //wurde save aufgerufen und den user gespeichert
        verify(userRepository).save(captor.capture());
        //den gespeicherten user abfragen
        AppUser saved = captor.getValue();

        assertThat(saved.username()).isEqualTo("admin");
        assertThat(saved.role()).isEqualTo(Role.ADMIN);
        assertThat(saved.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.password()).isNotEqualTo("geheim123");
        assertThat(passwordEncoder.matches("geheim123", saved.password())).isTrue();
    }

    @Test
    void run_doesNotCreateAdmin_whenAdminAlreadyExists() {
        // given
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);
        AdminInitializer initializer =
                new AdminInitializer(userRepository,
                        passwordEncoder,
                        "admin",
                        "geheim123");

        // when
        initializer.run(null);

        // then
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void run_doesNotCreateAdmin_whenPasswordIsEmpty() {
        // given
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        AdminInitializer initializer =
                new AdminInitializer(userRepository,
                        passwordEncoder,
                        "admin",
                        "");

        // when
        initializer.run(null);

        // then
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void run_doesNotCreateAdmin_whenUsernameIsEmpty() {
        // given
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        AdminInitializer initializer =
                new AdminInitializer(userRepository,
                        passwordEncoder,
                        "",
                        "geheim123");

        // when
        initializer.run(null);

        // then
        verify(userRepository, never()).save(any(AppUser.class));
    }
}