package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException;
import com.example.hexagonal.catalog.application.exception.InvalidUserDataException;
import com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException;
import com.example.hexagonal.catalog.domain.model.Authorities;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.example.hexagonal.catalog.domain.port.out.PasswordHasher;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCaseImpl(userRepository, passwordHasher);
    }

    private RegisterUserCommand valid() {
        return new RegisterUserCommand("Marina.R", "clave-segura", "Marina", "Rivadeneira",
                "Marina@Example.com", null, null);
    }

    @Test
    @DisplayName("Registra un usuario activo, con ROLE_USER, login y email en minuscula y contrasena hasheada")
    void shouldRegisterValidUser() {
        when(passwordHasher.hash("clave-segura")).thenReturn("hash-argon2");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = useCase.register(valid());

        assertEquals("marina.r", saved.getLogin());
        assertEquals("marina@example.com", saved.getEmail());
        assertEquals("hash-argon2", saved.getPasswordHash());
        assertEquals("Marina", saved.getFirstName());
        assertEquals("Rivadeneira", saved.getLastName());
        assertNull(saved.getImageUrl());
        assertTrue(saved.isActivated());
        assertEquals("es", saved.getLangKey());
        assertEquals(1, saved.getAuthorities().size());
        assertTrue(saved.getAuthorities().contains(Authorities.USER));
        assertEquals("system", saved.getCreatedBy());
        assertNotNull(saved.getCreatedDate());
    }

    @Test
    @DisplayName("Datos invalidos: informa cada campo y no toca la base ni el hasher")
    void shouldRejectInvalidData() {
        RegisterUserCommand invalid = new RegisterUserCommand("ab", "123", null, "x".repeat(51),
                "no-es-un-email", null, "e");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class, () -> useCase.register(invalid));

        assertTrue(ex.getFieldErrors().containsKey("login"));
        assertTrue(ex.getFieldErrors().containsKey("password"));
        assertTrue(ex.getFieldErrors().containsKey("email"));
        assertTrue(ex.getFieldErrors().containsKey("firstName"));
        assertTrue(ex.getFieldErrors().containsKey("lastName"));
        assertTrue(ex.getFieldErrors().containsKey("langKey"));
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("firstName y lastName son obligatorios")
    void shouldRejectMissingNames() {
        RegisterUserCommand command = new RegisterUserCommand("marina.r", "clave-segura", " ", null,
                "marina@example.com", null, null);

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class, () -> useCase.register(command));

        assertTrue(ex.getFieldErrors().containsKey("firstName"));
        assertTrue(ex.getFieldErrors().containsKey("lastName"));
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("Login con caracteres no permitidos o contrasena demasiado larga se rechazan")
    void shouldRejectInvalidLoginCharactersAndLongPassword() {
        RegisterUserCommand invalid = new RegisterUserCommand("mari na", "x".repeat(101), "Marina", "Rivadeneira",
                "marina@example.com", null, null);

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class, () -> useCase.register(invalid));

        assertTrue(ex.getFieldErrors().containsKey("login"));
        assertTrue(ex.getFieldErrors().containsKey("password"));
    }

    @Test
    @DisplayName("Login ya usado: lanza LoginAlreadyUsedException y no guarda")
    void shouldRejectDuplicatedLogin() {
        when(userRepository.existsByLogin(anyString()))
                .thenAnswer(invocation -> "marina.r".equals(invocation.getArgument(0)));

        assertThrows(LoginAlreadyUsedException.class, () -> useCase.register(valid()));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email ya usado: lanza EmailAlreadyUsedException y no guarda")
    void shouldRejectDuplicatedEmail() {
        when(userRepository.existsByEmail(anyString()))
                .thenAnswer(invocation -> "marina@example.com".equals(invocation.getArgument(0)));

        assertThrows(EmailAlreadyUsedException.class, () -> useCase.register(valid()));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un login igual al email de otro usuario se rechaza (evita confundir cuentas al ingresar)")
    void shouldRejectLoginEqualToAnotherUsersEmail() {
        when(userRepository.existsByEmail(anyString()))
                .thenAnswer(invocation -> "ana@example.com".equals(invocation.getArgument(0)));
        RegisterUserCommand command = new RegisterUserCommand("ana@example.com", "clave-segura", "Ana", "Perez",
                "otra@example.com", null, null);

        assertThrows(LoginAlreadyUsedException.class, () -> useCase.register(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("El mismo texto como login y como email del propio usuario es valido")
    void shouldAllowSameTextAsLoginAndEmailOfTheSameUser() {
        when(passwordHasher.hash(anyString())).thenReturn("hash-argon2");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RegisterUserCommand command = new RegisterUserCommand("ana@example.com", "clave-segura", "Ana", "Perez",
                "ana@example.com", null, null);

        User saved = useCase.register(command);

        assertEquals("ana@example.com", saved.getLogin());
        assertEquals("ana@example.com", saved.getEmail());
    }
}
