package com.example.hexagonal.catalog.infrastructure.web;

import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requiere el compose activo (PostgreSQL). Cada test se deshace al terminar (@Transactional). */
@SpringBootTest
@Transactional
class AuthenticationApiIntegrationTest {

    private static final String BEARER = "Bearer ";
    private static final String PASSWORD = "clave-segura";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- helpers ----------

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String registerJson(String login, String email, String password) {
        return """
                {"login": "%s", "password": "%s", "firstName": "Marina", "lastName": "Rivadeneira",
                 "email": "%s", "langKey": "es"}
                """.formatted(login, password, email);
    }

    private ResultActions registerRaw(String login, String email) throws Exception {
        return postJson("/api/register", registerJson(login, email, PASSWORD));
    }

    private void register(String login, String email) throws Exception {
        registerRaw(login, email).andExpect(status().isCreated());
    }

    private ResultActions authenticateRaw(String username, String password) throws Exception {
        return postJson("/api/authenticate", """
                {"username": "%s", "password": "%s", "rememberMe": false}
                """.formatted(username, password));
    }

    private String login(String username, String password) throws Exception {
        String body = authenticateRaw(username, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id_token");
    }

    private ResultActions getWithToken(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, BEARER + token));
    }

    private String token(JwtEncoder encoder, MacAlgorithm algorithm, Instant issuedAt, Instant expiresAt,
                         String authorities, long userId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("alguien")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("auth", authorities)
                .claim("userId", userId)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims)).getTokenValue();
    }

    private String validToken(String authorities, long userId) {
        Instant now = Instant.now();
        return token(jwtEncoder, MacAlgorithm.HS512, now, now.plus(1, ChronoUnit.HOURS), authorities, userId);
    }

    private void assertUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ---------- registro, login y cuenta ----------

    @Test
    @DisplayName("Registro, login y cuenta propia: el hash nunca viaja en las respuestas")
    void shouldRegisterAuthenticateAndReadAccount() throws Exception {
        String login = "mari-" + unique();
        String email = login + "@example.com";
        register(login, email);

        String authBody = authenticateRaw(login, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(result -> assertTrue(
                        result.getResponse().getHeader(HttpHeaders.AUTHORIZATION).startsWith(BEARER)))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(authBody, "$.id_token");

        String account = getWithToken("/api/account", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(account.contains("password"));
        assertFalse(account.contains("argon2"));
    }

    @Test
    @DisplayName("Se puede ingresar con el email y con el login en mayusculas")
    void shouldAuthenticateByEmailAndIgnoreCase() throws Exception {
        String login = "mari-" + unique();
        String email = login + "@example.com";
        register(login, email);

        authenticateRaw(email, PASSWORD).andExpect(status().isOk());
        authenticateRaw(login.toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("La identidad sale del token: un userId enviado en la URL se ignora")
    void shouldIgnoreUserIdSentByTheClient() throws Exception {
        String loginA = "ana-" + unique();
        String loginB = "bea-" + unique();
        register(loginA, loginA + "@example.com");
        register(loginB, loginB + "@example.com");
        Long idOfB = userRepository.findByLogin(loginB).orElseThrow().getId();
        String tokenOfA = login(loginA, PASSWORD);

        getWithToken("/api/account?userId=" + idOfB, tokenOfA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(loginA));
    }

    // ---------- registro invalido ----------

    @Test
    @DisplayName("Datos invalidos: 400 VALIDATION_ERROR con el detalle por campo")
    void shouldRejectInvalidRegistration() throws Exception {
        postJson("/api/register", "{\"login\": \"ab\", \"password\": \"123\", \"email\": \"no-es-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.login").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("Login o email repetidos: 400 con el code correspondiente")
    void shouldRejectDuplicatedLoginAndEmail() throws Exception {
        String login = "dup-" + unique();
        String email = login + "@example.com";
        register(login, email);

        registerRaw(login, "otro-" + unique() + "@example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_ALREADY_USED"));
        registerRaw("otro-" + unique(), email)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON: 400 BAD_REQUEST")
    void shouldRejectMalformedJson() throws Exception {
        postJson("/api/register", "esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ---------- login incorrecto ----------

    @Test
    @DisplayName("Contrasena incorrecta y usuario inexistente dan exactamente la misma respuesta 401")
    void shouldGiveSameResponseForWrongPasswordAndUnknownUser() throws Exception {
        String login = "mari-" + unique();
        register(login, login + "@example.com");

        String wrongPassword = authenticateRaw(login, "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();
        String unknownUser = authenticateRaw("fantasma-" + unique(), "otra-clave")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    @DisplayName("Una cuenta inactiva no puede ingresar")
    void shouldRejectInactiveAccount() throws Exception {
        String login = "mari-" + unique();
        register(login, login + "@example.com");
        User user = userRepository.findByLogin(login).orElseThrow();
        user.setActivated(false);
        userRepository.save(user);

        authenticateRaw(login, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ---------- acceso sin token o con token invalido ----------

    @Test
    @DisplayName("Sin token: 401 en problem+json, con WWW-Authenticate sin el motivo")
    void shouldRejectRequestsWithoutToken() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/account")));
        assertUnauthorized(mockMvc.perform(get("/api/ruta-inexistente")));
    }

    @Test
    @DisplayName("Un token mal formado se rechaza")
    void shouldRejectMalformedToken() throws Exception {
        assertUnauthorized(getWithToken("/api/account", "esto-no-es-un-jwt"));
    }

    @Test
    @DisplayName("Un token vencido se rechaza")
    void shouldRejectExpiredToken() throws Exception {
        Instant now = Instant.now();
        String expired = token(jwtEncoder, MacAlgorithm.HS512,
                now.minus(3, ChronoUnit.HOURS), now.minus(2, ChronoUnit.HOURS), "ROLE_USER", 1L);

        assertUnauthorized(getWithToken("/api/account", expired));
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void shouldRejectTokenSignedWithAnotherKey() throws Exception {
        byte[] otherKey = new byte[64];
        Arrays.fill(otherKey, (byte) 7);
        JwtEncoder otherEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA512")));
        Instant now = Instant.now();
        String foreign = token(otherEncoder, MacAlgorithm.HS512, now, now.plus(1, ChronoUnit.HOURS), "ROLE_USER", 1L);

        assertUnauthorized(getWithToken("/api/account", foreign));
    }

    @Test
    @DisplayName("Un token HS256 con la clave correcta se rechaza: solo se acepta HS512")
    void shouldRejectTokenWithAnotherAlgorithm() throws Exception {
        Instant now = Instant.now();
        String hs256 = token(jwtEncoder, MacAlgorithm.HS256, now, now.plus(1, ChronoUnit.HOURS), "ROLE_USER", 1L);

        assertUnauthorized(getWithToken("/api/account", hs256));
    }

    @Test
    @DisplayName("Un token sin firma (alg none) se rechaza")
    void shouldRejectUnsignedToken() throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString("{\"sub\":\"x\",\"userId\":1}".getBytes(StandardCharsets.UTF_8));

        assertUnauthorized(getWithToken("/api/account", header + "." + payload + "."));
    }

    @Test
    @DisplayName("Un token con el cuerpo alterado se rechaza")
    void shouldRejectTamperedToken() throws Exception {
        String valid = validToken("ROLE_USER", 5L);
        String[] parts = valid.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String altered = payload.replace("\"userId\":5", "\"userId\":6");
        assertNotEquals(payload, altered, "el test debe alterar realmente el cuerpo del token");
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(altered.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertUnauthorized(getWithToken("/api/account", tampered));
    }

    // ---------- autorizacion y errores estandar ----------

    @Test
    @DisplayName("Un usuario sin el rol pedido recibe 403; con el rol, 200")
    void shouldForbidWhenRoleIsMissing() throws Exception {
        getWithToken("/api/test/admin-only", validToken("ROLE_USER", 1L))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        getWithToken("/api/test/admin-only", validToken("ROLE_ADMIN", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ruta inexistente y metodo no permitido: mismo formato, con code estable")
    void shouldTranslateStandardSpringErrors() throws Exception {
        String token = validToken("ROLE_USER", 1L);

        getWithToken("/api/ruta-inexistente", token)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        getWithToken("/api/register", token)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
