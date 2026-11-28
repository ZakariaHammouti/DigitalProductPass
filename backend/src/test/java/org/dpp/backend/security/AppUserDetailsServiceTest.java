package org.dpp.backend.security;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.dpp.backend.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AppUserDetailsService service = new AppUserDetailsService(userRepository);

    @Test
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        // given
        AppUser appUser = AppUser.builder()
                .id("1")
                .username("admin")
                .password("hash123")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(appUser));

        // when
        UserDetails userDetails = service.loadUserByUsername("admin");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("hash123");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserDoesNotExist() {
        // given
        when(userRepository.findByUsername("unbekannt")).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.loadUserByUsername("unbekannt"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void loadUserByUsername_mapsRoleToAuthority(Role role) {
        // given
        AppUser appUser = AppUser.builder()
                .id("1")
                .username("user")
                .password("hash123")
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(appUser));

        // when
        UserDetails userDetails = service.loadUserByUsername("user");

        // then
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + role.name());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
        // Account Status     isEnabled  isAccountNonLocked
        //ACTIVE	            true	    true
        //PENDING	            false	    true
        //REJECTED	            true	    false
        //LOCKED	            true	    false
    void loadUserByUsername_mapsAccountStatus(AccountStatus status) {
        AppUser appUser = AppUser.builder()
                .id("1").username("user").password("hash123")
                .role(Role.MANUFACTURER).accountStatus(status)
                .build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(appUser));

        UserDetails userDetails = service.loadUserByUsername("user");

        assertThat(userDetails.isEnabled()).isEqualTo(status != AccountStatus.PENDING);
        assertThat(userDetails.isAccountNonLocked())
                .isEqualTo(status != AccountStatus.REJECTED && status != AccountStatus.LOCKED);
    }
}