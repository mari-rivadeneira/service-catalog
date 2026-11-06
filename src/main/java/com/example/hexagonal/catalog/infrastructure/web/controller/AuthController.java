package com.example.hexagonal.catalog.infrastructure.web.controller;

import com.example.hexagonal.catalog.domain.port.in.AuthenticateUserUseCase;
import com.example.hexagonal.catalog.domain.port.in.AuthenticateUserUseCase.AuthenticateCommand;
import com.example.hexagonal.catalog.domain.port.in.RegisterUserUseCase;
import com.example.hexagonal.catalog.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.example.hexagonal.catalog.infrastructure.web.dto.AuthenticateRequest;
import com.example.hexagonal.catalog.infrastructure.web.dto.RegisterRequest;
import com.example.hexagonal.catalog.infrastructure.web.dto.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    /** Como en JHipster: 201 sin cuerpo. El usuario queda activo y puede ingresar de inmediato. */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        registerUserUseCase.register(new RegisterUserCommand(request.login(), request.password(),
                request.firstName(), request.lastName(), request.email(), request.imageUrl(), request.langKey()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<TokenResponse> authenticate(@RequestBody AuthenticateRequest request) {
        String token = authenticateUserUseCase.authenticate(new AuthenticateCommand(
                request.username(), request.password(), Boolean.TRUE.equals(request.rememberMe())));
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new TokenResponse(token));
    }
}