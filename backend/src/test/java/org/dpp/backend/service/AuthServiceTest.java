package org.dpp.backend.service;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.dpp.backend.user.RegisterRequestDto;
import org.dpp.backend.user.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService authService = new AuthService(userRepository, passwordEncoder);

    @Test
    void register_savesUserWithHashedPasswordAndPendingStatus() {
        when(passwordEncoder.encode("Geheim1!")).thenReturn("HASH");

        authService.register(new RegisterRequestDto(
                "testrecycler", "test@recycling.de", "Geheim1!", "Test Recycling GmbH", Role.RECYCLER));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.password()).isEqualTo("HASH");
        assertThat(saved.accountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(saved.username()).isEqualTo("testrecycler");
        assertThat(saved.email()).isEqualTo("test@recycling.de");
        assertThat(saved.company()).isEqualTo("Test Recycling GmbH");
        assertThat(saved.role()).isEqualTo(Role.RECYCLER);
        assertThat(saved.id()).isNotBlank();
    }

    @Test
    void register_createsNewIdForEveryUser() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "testrecycler", "test@recycling.de", "Geheim1!", "Test Recycling GmbH", Role.RECYCLER);

        authService.register(dto);
        authService.register(dto);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository, times(2)).save(captor.capture());
        List<AppUser> saved = captor.getAllValues();

        assertThat(saved.get(0).id()).isNotEqualTo(saved.get(1).id());
    }
}
