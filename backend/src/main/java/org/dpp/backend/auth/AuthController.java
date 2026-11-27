package org.dpp.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AppUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public CurrentUserResponseDto login(@RequestBody
                                        @Valid
                                        LoginRequestDto loginRequestDto,
                                        HttpServletRequest httpServletRequest,
                                        HttpServletResponse httpServletResponse) {

        // Authentifizieren eines nicht geprüften Benutzers mit dem AuthenticationManager
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginRequestDto.username(),
                loginRequestDto.password()
        );
        Authentication authentication = authenticationManager.authenticate(authToken);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);

        AppUser appUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden: " + authentication.getName()));

        CurrentUserResponseDto currentUserResponseDto = new CurrentUserResponseDto(
                appUser.username(),
                appUser.role()
        );

        return currentUserResponseDto;
    }
}
