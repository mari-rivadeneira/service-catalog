package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    /** @throws com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException
     *  @throws com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException */
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}