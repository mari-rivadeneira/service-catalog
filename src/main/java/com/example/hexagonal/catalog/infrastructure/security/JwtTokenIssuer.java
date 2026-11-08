package com.example.hexagonal.catalog.infrastructure.security;

import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.out.TokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenValidity;
    private final Duration rememberMeValidity;

    public JwtTokenIssuer(JwtEncoder jwtEncoder,
                          @Value("${jwt.token-validity:PT24H}") Duration tokenValidity,
                          @Value("${jwt.remember-me-validity:P30D}") Duration rememberMeValidity) {
        this.jwtEncoder = jwtEncoder;
        this.tokenValidity = tokenValidity;
        this.rememberMeValidity = rememberMeValidity;
    }

    @Override
    public String issue(User user, boolean rememberMe) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(rememberMe ? rememberMeValidity : tokenValidity);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getLogin())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtClaims.AUTH, user.getAuthorities().stream().sorted().collect(Collectors.joining(" ")))
                .claim(JwtClaims.USER_ID, user.getId())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
