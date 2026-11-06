package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.User;

public interface RegisterUserUseCase {

    //Registra un usuario final: queda activo de inmediato, con ROLE_USER y la contrasena hasheada.
    User register(RegisterUserCommand command);

    record RegisterUserCommand(String login, String password, String firstName, String lastName,
                               String email, String imageUrl, String langKey) {
    }
}