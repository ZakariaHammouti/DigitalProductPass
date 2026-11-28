package org.dpp.backend.auth;

import org.dpp.backend.repository.UserRepository;
import org.dpp.backend.user.AccountStatus;
import org.dpp.backend.user.AppUser;
import org.dpp.backend.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",          // fast alles fehlt
            "geheim1!",     // kein Großbuchstabe
            "GEHEIM1!",     // kein Kleinbuchstabe
            "Geheimes!",    // keine Ziffer
            "Geheim12",     // kein Sonderzeichen
            "Ge1!",         // zu kurz
            ""              // leer
    })

        //############ Unit Tests Post /api/auth/register
    void register_returns400_whenPasswordIsWeak(String weakPassword) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testrecycler",
                                  "email": "test@recycling.de",
                                  "password": "%s",
                                  "company": "Test Recycling GmbH",
                                  "role": "RECYCLER"
                                }
                                """.formatted(weakPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_returns201_andSavesPendingUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testrecycler",
                                  "email": "test@recycling.de",
                                  "password": "Geheim1!",
                                  "company": "Test Recycling GmbH",
                                  "role": "RECYCLER"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertThat(saved.username()).isEqualTo("testrecycler");
        assertThat(saved.accountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(saved.role()).isEqualTo(Role.RECYCLER);
        assertThat(saved.password()).isNotEqualTo("Geheim1!");                       // kein Klartext
        assertThat(passwordEncoder.matches("Geheim1!", saved.password())).isTrue(); // aber der richtige Hash
    }

    @Test
    void register_returns400_whenPasswordIsLongerThan72Bytes() throws Exception {
        String tooLong = "Geheim1!".repeat(9) + "x";   // 73 Bytes, erfüllt sonst alle Regeln

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password[0]").value("Passwort ist zu lang"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_returns201_whenPasswordIsExactly72Bytes() throws Exception {
        String exactly72 = "Geheim1!".repeat(9);   // 72 Bytes, genau an der Grenze

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(exactly72)))
                .andExpect(status().isCreated());
    }

    private String registerJson(String password) {
        return """
                {
                  "username": "testrecycler",
                  "email": "test@recycling.de",
                  "password": "%s",
                  "company": "Test Recycling GmbH",
                  "role": "RECYCLER"
                }
                """.formatted(password);
    }

    @ParameterizedTest
    @CsvSource({
            // username,     email,              password,   company,              leeres Feld
            "'',             test@recycling.de,  Geheim1!,   Test Recycling GmbH,  username",
            "testrecycler,   '',                 Geheim1!,   Test Recycling GmbH,  email",
            "testrecycler,   test@recycling.de,  '',         Test Recycling GmbH,  password",
            "testrecycler,   test@recycling.de,  Geheim1!,   '',                   company"
    })
    void register_returns400_whenFieldIsBlank(String username, String email, String password,
                                              String company, String blankField) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "company": "%s",
                                  "role": "RECYCLER"
                                }
                                """.formatted(username, email, password, company)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + blankField).exists());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_returns400_whenRoleIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testrecycler",
                                  "email": "test@recycling.de",
                                  "password": "Geheim1!",
                                  "company": "Test Recycling GmbH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role[0]").value("Rolle darf nicht leer sein"));

        verify(userRepository, never()).save(any());
    }
}