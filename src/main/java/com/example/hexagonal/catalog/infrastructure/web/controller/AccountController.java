package com.example.hexagonal.catalog.infrastructure.web.controller;

import com.example.hexagonal.catalog.domain.port.in.GetAccountUseCase;
import com.example.hexagonal.catalog.infrastructure.security.JwtClaims;
import com.example.hexagonal.catalog.infrastructure.web.dto.AccountResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final GetAccountUseCase getAccountUseCase;

    public AccountController(GetAccountUseCase getAccountUseCase) {
        this.getAccountUseCase = getAccountUseCase;
    }

    /** La identidad sale del token, nunca de un parametro del pedido. */
    @GetMapping("/account")
    public AccountResponse account(@AuthenticationPrincipal Jwt jwt) {
        Number userId = jwt.getClaim(JwtClaims.USER_ID);
        if (userId == null) {
            throw new AccessDeniedException("El token no identifica a un usuario");
        }
        return AccountResponse.from(getAccountUseCase.getAccount(userId.longValue()));
    }
}
