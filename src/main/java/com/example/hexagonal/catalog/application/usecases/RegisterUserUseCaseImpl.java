package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException;
import com.example.hexagonal.catalog.application.exception.InvalidUserDataException;
import com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException;
import com.example.hexagonal.catalog.domain.model.Authorities;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.in.RegisterUserUseCase;
import com.example.hexagonal.catalog.domain.port.out.PasswordHasher;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[_.@A-Za-z0-9-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String DEFAULT_LANG_KEY = "es";
    private static final String CREATED_BY = "system";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCaseImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(RegisterUserCommand command) {
        validate(command);

        String login = command.login().toLowerCase(Locale.ROOT);
        String email = command.email().toLowerCase(Locale.ROOT);

        // Un texto no puede ser el login de un usuario y el email de otro: se confundirian al ingresar
        if (userRepository.existsByLogin(login) || userRepository.existsByEmail(login)) {
            throw new LoginAlreadyUsedException(login);
        }
        if (userRepository.existsByEmail(email) || userRepository.existsByLogin(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordHasher.hash(command.password()));
        user.setFirstName(blankToNull(command.firstName()));
        user.setLastName(blankToNull(command.lastName()));
        user.setEmail(email);
        user.setImageUrl(blankToNull(command.imageUrl()));
        user.setActivated(true);
        user.setLangKey(isBlank(command.langKey()) ? DEFAULT_LANG_KEY : command.langKey());
        user.setAuthorities(new HashSet<>(Set.of(Authorities.USER)));
        user.setCreatedBy(CREATED_BY);
        user.setCreatedDate(Instant.now());

        return userRepository.save(user);
    }

    private void validate(RegisterUserCommand command) {
        Map<String, String> errors = new LinkedHashMap<>();

        String login = command.login();
        if (isBlank(login)) {
            errors.put("login", "es obligatorio");
        } else if (login.length() < 3 || login.length() > 50) {
            errors.put("login", "debe tener entre 3 y 50 caracteres");
        } else if (!LOGIN_PATTERN.matcher(login).matches()) {
            errors.put("login", "solo admite letras, numeros y los caracteres _ . @ -");
        }

        String password = command.password();
        if (isBlank(password)) {
            errors.put("password", "es obligatoria");
        } else if (password.length() < 4 || password.length() > 100) {
            errors.put("password", "debe tener entre 4 y 100 caracteres");
        }

        String email = command.email();
        if (isBlank(email)) {
            errors.put("email", "es obligatorio");
        } else if (email.length() < 5 || email.length() > 254) {
            errors.put("email", "debe tener entre 5 y 254 caracteres");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "no tiene un formato valido");
        }

        if (command.firstName() != null && command.firstName().length() > 50) {
            errors.put("firstName", "no puede superar los 50 caracteres");
        }
        if (command.lastName() != null && command.lastName().length() > 50) {
            errors.put("lastName", "no puede superar los 50 caracteres");
        }
        if (command.imageUrl() != null && command.imageUrl().length() > 256) {
            errors.put("imageUrl", "no puede superar los 256 caracteres");
        }
        String langKey = command.langKey();
        if (!isBlank(langKey) && (langKey.length() < 2 || langKey.length() > 10)) {
            errors.put("langKey", "debe tener entre 2 y 10 caracteres");
        }

        if (!errors.isEmpty()) {
            throw new InvalidUserDataException(errors);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
