package com.example.hexagonal.catalog.domain.port.in;

public interface AuthenticateUserUseCase {

    /**
     * Autentica por login o email (sin distinguir mayusculas) y devuelve el token JWT.
     *
     * @throws com.example.hexagonal.catalog.application.exception.InvalidCredentialsException
     *         si el usuario no existe, la contrasena es incorrecta o la cuenta esta inactiva (mismo error en los tres casos)
     */
    String authenticate(AuthenticateCommand command);

    record AuthenticateCommand(String username, String password, boolean rememberMe) {
    }
}