package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidCredentialsException;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.in.AuthenticateUserUseCase;
import com.example.hexagonal.catalog.domain.port.out.PasswordHasher;
import com.example.hexagonal.catalog.domain.port.out.TokenIssuer;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private static final int MAX_IDENTIFIER_LENGTH = 254;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserUseCaseImpl(UserRepository userRepository,
                                       PasswordHasher passwordHasher,
                                       TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public String authenticate(AuthenticateCommand command) {
        String username = command.username();
        String password = command.password();

        // Entradas vacias o enormes se rechazan sin consultar la base ni ejecutar el hash (que es costoso)
        if (username == null || username.isBlank() || username.length() > MAX_IDENTIFIER_LENGTH
                || password == null || password.isEmpty() || password.length() > MAX_PASSWORD_LENGTH) {
            throw new InvalidCredentialsException();
        }

        String identifier = username.trim().toLowerCase(Locale.ROOT);

        for (User candidate : candidates(identifier)) {
            if (candidate.isActivated() && passwordHasher.matches(password, candidate.getPasswordHash())) {
                return tokenIssuer.issue(candidate, command.rememberMe());
            }
        }
        throw new InvalidCredentialsException();
    }

    /**
     * El mismo texto puede coincidir con el login de un usuario y con el email de otro (el registro lo previene,
     * pero dos registros simultaneos podrian colarse): se prueba la contrasena contra ambos, el login primero.
     */
    private List<User> candidates(String identifier) {
        List<User> found = new ArrayList<>();
        userRepository.findByLogin(identifier).ifPresent(found::add);
        userRepository.findByEmail(identifier)
                .filter(byEmail -> found.stream().noneMatch(user -> user.getId().equals(byEmail.getId())))
                .ifPresent(found::add);
        return found;
    }
}
