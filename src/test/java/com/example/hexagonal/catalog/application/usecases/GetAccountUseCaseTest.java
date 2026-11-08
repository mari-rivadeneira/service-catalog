package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Devuelve la cuenta del usuario pedido")
    void shouldReturnAccount() {
        User user = new User();
        user.setId(5L);
        user.setLogin("marina.r");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        User account = new GetAccountUseCaseImpl(userRepository).getAccount(5L);

        assertEquals("marina.r", account.getLogin());
    }

    @Test
    @DisplayName("Si el usuario no existe lanza EntityNotFoundException")
    void shouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> new GetAccountUseCaseImpl(userRepository).getAccount(99L));
    }
}