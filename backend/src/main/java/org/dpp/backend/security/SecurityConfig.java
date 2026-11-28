package org.dpp.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.
                csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        //login sind für jede Anfrage offen
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        //sonst nur für angemeldete User
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // Nicht eingeloggt → 401 statt Weiterleitung (damit axios es erkennt)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Abmelden per POST /api/auth/logout
                .logout(l -> l
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.OK.value())));
        return http.build();
    }

    /**
     * Stellt den AuthenticationManager bereit, den der AuthController beim Login aufruft.
     * --
     * Diese Methode läuft nur EINMAL beim App-Start und baut den Manager. Die Prüfungen laufen erst bei jedem Login.
     * --
     * Ablauf bei authenticate(...) (Reihenfolge ist in Spring fest vorgegeben):
     * 1. Benutzer laden (AppUserDetailsService)
     * 2. PRE-Prüfung
     * 3. Passwort mit BCrypt vergleichen
     * 4. POST-Prüfung
     * --
     * Standardmäßig prüft Spring den Kontostatus (gesperrt/deaktiviert) schon in der PRE-Prüfung,
     * also VOR dem Passwort. Dann bekäme auch jemand mit falschem Passwort z. B. "wartet auf Freigabe"
     * und wüsste, dass der Benutzername existiert.
     * Deshalb: PRE-Prüfung leer, Statusprüfung erst in der POST-Prüfung, also NACH dem Passwort (AK 2).
     * Die Reihenfolge selbst gibt Spring vor; hier wird nur festgelegt, WAS in PRE und WAS in POST geprüft wird.
     */
    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        // Provider bekommt die Werkzeuge: den Service zum Laden der Benutzer und BCrypt zum Passwortvergleich
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        // PRE: legt fest, was VOR dem Passwortvergleich geprüft wird. Hier: nichts.
        provider.setPreAuthenticationChecks(user -> {
        });

        // POST: legt fest, was NACH richtigem Passwort geprüft wird. Hier: der Kontostatus
        // (deaktiviert → DisabledException, gesperrt → LockedException; Werte kommen aus den Flags im AppUserDetailsService)
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());

        // ProviderManager ist Springs Standard-Implementierung des AuthenticationManager
        return new ProviderManager(provider);
    }
}
