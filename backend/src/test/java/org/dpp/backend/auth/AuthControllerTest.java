package org.dpp.backend.auth;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.dpp.backend.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.clearInvocations;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        clearInvocations(userRepository);
    }

    @Test
    void login_returns400_whenUsernameIsTooLong() throws Exception {
        String tooLong = "a".repeat(51);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "geheim123"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username[0]").value("Benutzername darf höchstens 50 Zeichen haben"));

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void login_returns400_whenPasswordIsTooLong() throws Exception {
        String tooLong = "a".repeat(73);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "%s"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password[0]").value("Passwort ist zu lang"));

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void protectedEndpoint_returns401_whenNotLoggedIn() throws Exception {
        //Test, dass ein geschützter Endpunkt 401 zurückgibt,
        // wenn der Benutzer nicht angemeldet ist
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returnsUserAndCreatesSession_whenCredentialsAreCorrect() throws Exception {
        // given
        AppUser admin = AppUser.builder()
                .id("1")
                .username("admin")
                .password(passwordEncoder.encode("geheim123"))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // when + then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "geheim123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()));
    }

    @Test
    void login_returns401WithMessage_whenPasswordIsWrong() throws Exception {
        // given
        AppUser admin = AppUser.builder()
                .id("1")
                .username("admin")
                .password(passwordEncoder.encode("geheim123"))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // when + then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "falsch"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Benutzername oder Passwort falsch"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void login_returns401WithSameMessage_whenUserDoesNotExist() throws Exception {
        // given
        when(userRepository.findByUsername("unbekannt")).thenReturn(Optional.empty());

        // when + then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "unbekannt", "password": "egal123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Benutzername oder Passwort falsch"))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void login_returns400WithFieldErrors_whenFieldsAreBlank() throws Exception {
        // when + then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Eingaben sind ungültig"))
                .andExpect(jsonPath("$.errors.username[0]").value("Benutzername darf nicht leer sein"))
                .andExpect(jsonPath("$.errors.password[0]").value("Passwort darf nicht leer sein"));

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void login_returns500WithGeneralMessage_whenDatabaseFails() throws Exception {
        // given
        when(userRepository.findByUsername("admin"))
                .thenThrow(new RuntimeException("Datenbank nicht erreichbar"));

        // when + then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "geheim123"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Es ist ein Fehler aufgetreten. Bitte versuche es später erneut."))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void logout_returns200AndInvalidatesSession() throws Exception {
        // given: angemeldeter Admin
        AppUser admin = AppUser.builder()
                .id("1")
                .username("admin")
                .password(passwordEncoder.encode("geheim123"))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "geheim123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // when: abmelden mit derselben Session
        mockMvc.perform(post("/api/auth/logout").session(session))
                // then
                .andExpect(status().isOk());

        assertThat(session.isInvalid()).isTrue();
    }

    private MockHttpSession loginAsAdmin() throws Exception {
        AppUser admin = AppUser.builder()
                .id("1")
                .username("admin")
                .password(passwordEncoder.encode("geheim123"))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "geheim123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    @Test
    void me_returnsCurrentUser_whenLoggedIn() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void me_returns401_afterLogout() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private void givenUser(AccountStatus status) {
        AppUser user = AppUser.builder()
                .id("2").username("hersteller")
                .password(passwordEncoder.encode("geheim123"))
                .role(Role.MANUFACTURER).accountStatus(status)
                .build();
        when(userRepository.findByUsername("hersteller")).thenReturn(Optional.of(user));
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "hersteller", "password": "%s"}
                        """.formatted(password)));
    }

    @Test
    void login_returnsPendingMessage_whenAccountPendingAndPasswordCorrect() throws Exception {
        givenUser(AccountStatus.PENDING);
        login("geheim123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Dein Konto wartet noch auf Freigabe"));
    }

    @Test
    void login_returnsGeneralMessage_whenAccountPendingAndPasswordWrong() throws Exception {
        givenUser(AccountStatus.PENDING);
        login("falsch")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Benutzername oder Passwort falsch"));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"REJECTED", "LOCKED"})
    void login_returnsLockedMessage_whenAccountRejectedOrLocked(AccountStatus status) throws Exception {
        givenUser(status);
        login("geheim123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Dein Konto ist gesperrt oder wurde abgelehnt"));
    }
}