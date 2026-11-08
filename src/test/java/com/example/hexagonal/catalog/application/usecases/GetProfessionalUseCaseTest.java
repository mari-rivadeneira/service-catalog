package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfessionalUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Test
    @DisplayName("Devuelve el profesional pedido")
    void shouldReturnProfessional() {
        Professional professional = new Professional(15L, 1L, "Ana", "Gomez", true, Instant.now(), Instant.now());
        when(professionalRepository.findById(15L)).thenReturn(Optional.of(professional));

        assertEquals("Gomez", new GetProfessionalUseCaseImpl(professionalRepository).getById(15L).getLastName());
    }

    @Test
    @DisplayName("Si no existe lanza EntityNotFoundException")
    void shouldFailWhenProfessionalDoesNotExist() {
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> new GetProfessionalUseCaseImpl(professionalRepository).getById(99L));
    }
}