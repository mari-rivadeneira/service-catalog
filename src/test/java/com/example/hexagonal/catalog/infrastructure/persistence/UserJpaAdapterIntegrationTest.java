package com.example.hexagonal.catalog.infrastructure.persistence;

import com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException;
import com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException;
import com.example.hexagonal.catalog.domain.model.Authorities;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserJpaAdapterIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private User user(String login, String email) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash("$argon2id$v=19$m=19456,t=2,p=1$hash-de-prueba");
        user.setFirstName("Marina");
        user.setEmail(email);
        user.setActivated(true);
        user.setLangKey("es");
        user.setAuthorities(new HashSet<>(Set.of(Authorities.USER)));
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        return user;
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Guarda un usuario con su autoridad y lo recupera por id, login y email")
    void shouldSaveAndFindUser() {
        String suffix = unique();
        User saved = userRepository.save(user("test-" + suffix, "test-" + suffix + "@example.com"));
        assertNotNull(saved.getId());
        flushAndClear();

        User byId = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals("test-" + suffix, byId.getLogin());
        assertEquals("Marina", byId.getFirstName());
        assertTrue(byId.isActivated());
        assertTrue(byId.getAuthorities().contains(Authorities.USER));

        assertTrue(userRepository.findByLogin("test-" + suffix).isPresent());
        assertTrue(userRepository.findByEmail("test-" + suffix + "@example.com").isPresent());
        assertTrue(userRepository.existsByLogin("test-" + suffix));
        assertTrue(userRepository.existsByEmail("test-" + suffix + "@example.com"));
        assertFalse(userRepository.existsByLogin("no-existe-" + suffix));
    }

    @Test
    @DisplayName("La restriccion unica de login se traduce a LoginAlreadyUsedException")
    void shouldTranslateDuplicatedLogin() {
        String suffix = unique();
        userRepository.save(user("dup-" + suffix, "uno-" + suffix + "@example.com"));

        assertThrows(LoginAlreadyUsedException.class,
                () -> userRepository.save(user("dup-" + suffix, "dos-" + suffix + "@example.com")));
    }

    @Test
    @DisplayName("La restriccion unica de email se traduce a EmailAlreadyUsedException")
    void shouldTranslateDuplicatedEmail() {
        String suffix = unique();
        userRepository.save(user("uno-" + suffix, "dup-" + suffix + "@example.com"));

        assertThrows(EmailAlreadyUsedException.class,
                () -> userRepository.save(user("dos-" + suffix, "dup-" + suffix + "@example.com")));
    }
}
