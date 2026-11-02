package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.port.in.SyncCatalogIncrementalUseCase;
import com.example.hexagonal.catalog.domain.port.out.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCatalogUpdatedEventUseCaseTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private SyncCatalogIncrementalUseCase syncCatalogIncrementalUseCase;

    private ProcessCatalogUpdatedEventUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessCatalogUpdatedEventUseCaseImpl(processedEventRepository, syncCatalogIncrementalUseCase);
    }

    @Test
    @DisplayName("Un evento duplicado no repite la sincronizacion")
    void shouldIgnoreDuplicatedEvent() {
        when(processedEventRepository.exists("evt-1")).thenReturn(true);

        useCase.process("evt-1", 7L);

        verifyNoInteractions(syncCatalogIncrementalUseCase);
        verify(processedEventRepository, never()).markProcessed(anyString());
    }

    @Test
    @DisplayName("Un evento nuevo sincroniza y recien despues se registra como procesado")
    void shouldSyncAndThenMarkProcessed() {
        when(processedEventRepository.exists("evt-1")).thenReturn(false);

        useCase.process("evt-1", 7L);

        InOrder order = inOrder(syncCatalogIncrementalUseCase, processedEventRepository);
        order.verify(syncCatalogIncrementalUseCase).sync(7L);
        order.verify(processedEventRepository).markProcessed("evt-1");
    }

    @Test
    @DisplayName("Si la sincronizacion falla, el evento NO se registra y podra reprocesarse")
    void shouldNotMarkProcessedWhenSyncFails() {
        when(processedEventRepository.exists("evt-1")).thenReturn(false);
        doThrow(new IllegalStateException("Redis caido")).when(syncCatalogIncrementalUseCase).sync(7L);

        assertThrows(IllegalStateException.class, () -> useCase.process("evt-1", 7L));

        verify(processedEventRepository, never()).markProcessed(anyString());
    }
}
