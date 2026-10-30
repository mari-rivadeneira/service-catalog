package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyCatalogSnapshotUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Mock
    private SyncVersionRepository syncVersionRepository;

    private ApplyCatalogSnapshotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyCatalogSnapshotUseCaseImpl(
                categoryRepository,
                professionalRepository,
                weeklyScheduleRepository,
                syncVersionRepository
        );
    }

    @Test
    @DisplayName("Debe persistir categorias, profesionales, agendas y actualizar sync_version")
    void shouldApplySnapshotSuccessfully() {
        ProfessionalCategory category = new ProfessionalCategory();
        Professional professional = new Professional();
        WeeklySchedule schedule = new WeeklySchedule();

        CatedraSnapshot snapshot = new CatedraSnapshot(
                10L,
                List.of(category),
                List.of(professional),
                List.of(schedule)
        );

        when(syncVersionRepository.findByEntityName("CATALOG")).thenReturn(Optional.empty());

        useCase.applySnapshot(snapshot);

        verify(categoryRepository, times(1)).save(category);
        verify(professionalRepository, times(1)).save(professional);
        verify(weeklyScheduleRepository, times(1)).saveAll(snapshot.getSchedules());

        ArgumentCaptor<SyncVersion> captor = ArgumentCaptor.forClass(SyncVersion.class);
        verify(syncVersionRepository, times(1)).save(captor.capture());

        SyncVersion savedVersion = captor.getValue();
        assertEquals("CATALOG", savedVersion.getEntityName());
        assertEquals(10L, savedVersion.getVersion());
        assertNotNull(savedVersion.getLastSyncedAt());
    }

    @Test
    @DisplayName("Debe fallar si el snapshot recibido es nulo")
    void shouldThrowExceptionWhenSnapshotIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.applySnapshot(null));
        verifyNoInteractions(categoryRepository, professionalRepository, weeklyScheduleRepository, syncVersionRepository);
    }
}