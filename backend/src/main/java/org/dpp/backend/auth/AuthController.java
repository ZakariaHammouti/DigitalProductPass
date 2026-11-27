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
import org.springframework.web.bind.annotation.*;

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

        return toCurrentUser(authentication.getName());
    }

    //Spring übergibt automatisch das Authentication-Objekt des aktuell angemeldeten Benutzers
    @GetMapping("/me")
    public CurrentUserResponseDto getCurrentUser(Authentication authentication) {
        return toCurrentUser(authentication.getName());
    }

    private CurrentUserResponseDto toCurrentUser(String username) {
        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden: " + username));
        return new CurrentUserResponseDto(appUser.username(), appUser.role());
    }
}
