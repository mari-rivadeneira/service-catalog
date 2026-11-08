package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidCredentialsException;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.in.AuthenticateUserUseCase.AuthenticateCommand;
import com.example.hexagonal.catalog.domain.port.out.PasswordHasher;
import com.example.hexagonal.catalog.domain.port.out.TokenIssuer;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private AuthenticateUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCaseImpl(userRepository, passwordHasher, tokenIssuer);
    }

    private User user(long id, String login, String email, boolean activated) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setEmail(email);
        user.setPasswordHash("hash-" + id);
        user.setActivated(activated);
        return user;
    }

    @Test
    @DisplayName("Credenciales correctas por login: devuelve el token, sin distinguir mayusculas ni espacios")
    void shouldAuthenticateByLogin() {
        User marina = user(1L, "marina.r", "marina@example.com", true);
        when(userRepository.findByLogin("marina.r")).thenReturn(Optional.of(marina));
        when(passwordHasher.matches("clave-segura", "hash-1")).thenReturn(true);
        when(tokenIssuer.issue(marina, true)).thenReturn("token-1");

        String token = useCase.authenticate(new AuthenticateCommand("  Marina.R ", "clave-segura", true));

        assertEquals("token-1", token);
    }

    @Test
    @DisplayName("Credenciales correctas por email")
    void shouldAuthenticateByEmail() {
        User marina = user(1L, "marina.r", "marina@example.com", true);
        when(userRepository.findByEmail("marina@example.com")).thenReturn(Optional.of(marina));
        when(passwordHasher.matches("clave-segura", "hash-1")).thenReturn(true);
        when(tokenIssuer.issue(marina, false)).thenReturn("token-1");

        assertEquals("token-1",
                useCase.authenticate(new AuthenticateCommand("marina@example.com", "clave-segura", false)));
    }

    @Test
    @DisplayName("Contrasena incorrecta: InvalidCredentialsException y no se emite token")
    void shouldRejectWrongPassword() {
        when(userRepository.findByLogin("marina.r"))
                .thenReturn(Optional.of(user(1L, "marina.r", "marina@example.com", true)));
        when(passwordHasher.matches("otra-clave", "hash-1")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("marina.r", "otra-clave", false)));

        verifyNoInteractions(tokenIssuer);
    }

    @Test
    @DisplayName("Usuario inexistente: el mismo error que una contrasena incorrecta")
    void shouldRejectUnknownUser() {
        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("fantasma", "clave-segura", false)));

        verifyNoInteractions(passwordHasher, tokenIssuer);
    }

    @Test
    @DisplayName("Cuenta inactiva: el mismo error, sin verificar la contrasena")
    void shouldRejectInactiveUser() {
        when(userRepository.findByLogin("marina.r"))
                .thenReturn(Optional.of(user(1L, "marina.r", "marina@example.com", false)));

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("marina.r", "clave-segura", false)));

        verifyNoInteractions(passwordHasher, tokenIssuer);
    }

    @Test
    @DisplayName("Si un texto es el login de uno y el email de otro, entra la cuenta cuya contrasena acierta")
    void shouldAuthenticateTheAccountWhosePasswordMatchesWhenLoginAndEmailCollide() {
        User byLogin = user(1L, "ana@example.com", "uno@example.com", true);
        User byEmail = user(2L, "ana", "ana@example.com", true);
        when(userRepository.findByLogin("ana@example.com")).thenReturn(Optional.of(byLogin));
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(byEmail));
        when(passwordHasher.matches("clave-de-la-segunda", "hash-1")).thenReturn(false);
        when(passwordHasher.matches("clave-de-la-segunda", "hash-2")).thenReturn(true);
        when(tokenIssuer.issue(byEmail, false)).thenReturn("token-2");

        assertEquals("token-2",
                useCase.authenticate(new AuthenticateCommand("ana@example.com", "clave-de-la-segunda", false)));
    }

    @Test
    @DisplayName("Un mismo usuario hallado por login y por email se verifica una sola vez (el hash es costoso)")
    void shouldVerifyTheSameUserOnlyOnce() {
        User same = user(1L, "ana@example.com", "ana@example.com", true);
        when(userRepository.findByLogin("ana@example.com")).thenReturn(Optional.of(same));
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(same));
        when(passwordHasher.matches("mala", "hash-1")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("ana@example.com", "mala", false)));

        verify(passwordHasher, times(1)).matches("mala", "hash-1");
    }

    @Test
    @DisplayName("Entradas vacias o enormes se rechazan sin consultar la base ni ejecutar el hash")
    void shouldRejectBlankOrHugeInputsWithoutTouchingAnything() {
        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand(" ", "clave", false)));
        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("marina", "", false)));
        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("marina", "x".repeat(101), false)));
        assertThrows(InvalidCredentialsException.class,
                () -> useCase.authenticate(new AuthenticateCommand("x".repeat(255), "clave", false)));

        verifyNoInteractions(userRepository, passwordHasher, tokenIssuer);
    }
}