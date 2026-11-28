package org.dpp.backend.service;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.dpp.backend.user.RegisterRequestDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequestDto registerRequestDto) {
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID().toString())
                .username(registerRequestDto.username())
                .email(registerRequestDto.email())
                .password(passwordEncoder.encode(registerRequestDto.password()))
                .role(registerRequestDto.role())
                .accountStatus(AccountStatus.PENDING)
                .company(registerRequestDto.company())
                .build();

        userRepository.save(user);
    }
}
