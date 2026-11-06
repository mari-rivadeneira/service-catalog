package com.example.hexagonal.catalog.infrastructure.security;

import com.example.hexagonal.catalog.domain.model.Authorities;
import com.example.hexagonal.catalog.domain.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenIssuerTest {

    private final SecretKey key = secretKey();

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)), Duration.ofHours(24), Duration.ofDays(30));

    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();

    private static SecretKey secretKey() {
        byte[] bytes = new byte[64];
        Arrays.fill(bytes, (byte) 5);
        return new SecretKeySpec(bytes, "HmacSHA512");
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setLogin("marina.r");
        user.setAuthorities(new HashSet<>(Set.of(Authorities.USER, Authorities.ADMIN)));
        return user;
    }

    @Test
    @DisplayName("El token va firmado con HS512 y lleva login, id, autoridades ordenadas y vencimiento")
    void shouldIssueTokenWithExpectedClaims() {
        Jwt jwt = decoder.decode(issuer.issue(user(), false));

        assertEquals("HS512", String.valueOf(jwt.getHeaders().get("alg")));
        assertEquals("marina.r", jwt.getSubject());
        Number userId = jwt.getClaim(JwtClaims.USER_ID);
        assertEquals(42L, userId.longValue());
        assertEquals("ROLE_ADMIN ROLE_USER", jwt.getClaimAsString(JwtClaims.AUTH));
        assertEquals(Duration.ofHours(24), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    @DisplayName("Con rememberMe el token dura 30 dias")
    void shouldIssueLongLivedTokenWhenRememberMe() {
        Jwt jwt = decoder.decode(issuer.issue(user(), true));

        assertEquals(Duration.ofDays(30), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }
}