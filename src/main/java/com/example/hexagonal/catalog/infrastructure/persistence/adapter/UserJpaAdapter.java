package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException;
import com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.UserMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserJpaAdapter implements UserRepository {

    private final SpringDataUserRepository repository;
    private final UserMapper mapper;

    public UserJpaAdapter(SpringDataUserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos pasan el chequeo previo: la restriccion unica frena al segundo
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            if (message.contains("uk_users_login")) {
                throw new LoginAlreadyUsedException(user.getLogin());
            }
            if (message.contains("uk_users_email")) {
                throw new EmailAlreadyUsedException(user.getEmail());
            }
            throw e;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return repository.findByLogin(login).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByLogin(String login) {
        return repository.existsByLogin(login);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}