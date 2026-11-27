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

    // Speichert den SecurityContext in der HTTP-Session, damit der Login über diese Anfrage hinaus gilt.
    // Selbst mit new erzeugt, weil Spring dafür keine fertige Bean bereitstellt.
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
        // Bei jedem Login, ausgelöst von authenticate(...):
        // 1. User holen: Dein Service fragt MongoDB. --> AppUserDetailsService.loadUserByUsername(username)
        // 2. PRE: nichts prüfen
        // 3. Passwort vergleichen: mit BCrypt --> Spring prüft hier, ob das Passwort stimmt. Wenn nicht, wird eine Exception geworfen.
        // 4. POST: Status prüfen, er muss ACTIVE sein --> Spring prüft hier, ob der AccountStatus ACTIVE ist. Wenn nicht, wird eine Exception geworfen.
        // 5. Fertig: geprüfter Token mit Name und Rolle zurück an den Controller --> Spring liefert den geprüften Token zurück, der Name und Rolle enthält. Der Controller kann jetzt den Benutzer als angemeldet betrachten.
        // Geht ein Schritt schief, ist sofort Schluss mit einer Exception.
        Authentication authentication = authenticationManager.authenticate(authToken); //--> Spring prüft hier Benutzername + Passwort + AccountStatus in userRepository.findByUsername(username)

        // Leere Hülle für die Anmeldedaten anlegen
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        // Geprüften Benutzer (Name + Rolle) hineinlegen
        context.setAuthentication(authentication);
        // Gilt ab jetzt für diese Anfrage als angemeldet
        SecurityContextHolder.setContext(context);
        // In der Session speichern; Spring setzt dabei das Cookie JSESSIONID,
        // damit die nächsten Anfragen den Benutzer wiedererkennen
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
