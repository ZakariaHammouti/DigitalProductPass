package org.dpp.backend.security;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//Beim login aufgerufen
@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //aufgerufen von Spring Security, wenn ein User sich einloggt.
    // Spring Security ruft diese Methode auf, um die UserDetails zu laden.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = getUserByUsername(username);

        return User.builder()
                .username(appUser.username())
                .password(appUser.password())
                .roles(appUser.role().name())
                // Setzt die Flags für den Benutzerstatus basierend auf dem AccountStatus
                .disabled(appUser.accountStatus() == AccountStatus.PENDING)
                .accountLocked(appUser.accountStatus() == AccountStatus.REJECTED
                        || appUser.accountStatus() == AccountStatus.LOCKED)
                .build();
    }

    private AppUser getUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden: " + username));
    }
}
