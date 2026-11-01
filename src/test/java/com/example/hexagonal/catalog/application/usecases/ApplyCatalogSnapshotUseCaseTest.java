package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncMode;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    private CatedraSnapshot snapshotWithVersion(long version) {
        return new CatedraSnapshot(
                version,
                List.of(new ProfessionalCategory()),
                List.of(new Professional()),
                List.of(new WeeklySchedule())
        );
    }

    @Test
    @DisplayName("Debe reemplazar la copia local en orden de FK y registrar la version del snapshot")
    void shouldReplaceLocalCopyAndRegisterVersion() {
        CatedraSnapshot snapshot = snapshotWithVersion(10L);
        when(syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY)).thenReturn(Optional.empty());

        useCase.applySnapshot(snapshot);

        InOrder order = inOrder(weeklyScheduleRepository, professionalRepository,
                categoryRepository, syncVersionRepository);
        order.verify(weeklyScheduleRepository).deleteAll();
        order.verify(professionalRepository).deleteAll();
        order.verify(categoryRepository).deleteAll();
        order.verify(categoryRepository).saveAll(snapshot.getCategories());
        order.verify(professionalRepository).saveAll(snapshot.getProfessionals());
        order.verify(weeklyScheduleRepository).saveAll(snapshot.getSchedules());

        ArgumentCaptor<SyncVersion> captor = ArgumentCaptor.forClass(SyncVersion.class);
        order.verify(syncVersionRepository).save(captor.capture());

        SyncVersion saved = captor.getValue();
        assertEquals(SyncVersion.CATALOG_KEY, saved.getEntityName());
        assertEquals(10L, saved.getVersion());
        assertEquals(SyncMode.SNAPSHOT, saved.getLastMode());
        assertNotNull(saved.getLastSyncedAt());
    }

    @Test
    @DisplayName("Debe adoptar la version del snapshot aunque sea menor a la local y limpiar el error previo")
    void shouldAdoptSnapshotVersionEvenIfLowerThanLocal() {
        SyncVersion existing = new SyncVersion(SyncVersion.CATALOG_KEY, 50L, Instant.now(),
                SyncMode.INCREMENTAL, "fallo anterior");
        when(syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY)).thenReturn(Optional.of(existing));

        useCase.applySnapshot(snapshotWithVersion(10L));

        ArgumentCaptor<SyncVersion> captor = ArgumentCaptor.forClass(SyncVersion.class);
        verify(syncVersionRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getVersion());
        assertNull(captor.getValue().getLastError());
    }

    @Test
    @DisplayName("No debe registrar la version si falla el guardado")
    void shouldNotRegisterVersionWhenSaveFails() {
        CatedraSnapshot snapshot = snapshotWithVersion(10L);
        doThrow(new IllegalStateException("fallo de persistencia"))
                .when(professionalRepository).saveAll(snapshot.getProfessionals());

        assertThrows(IllegalStateException.class, () -> useCase.applySnapshot(snapshot));

        verify(syncVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar si el snapshot recibido es nulo")
    void shouldThrowExceptionWhenSnapshotIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.applySnapshot(null));
        verifyNoInteractions(categoryRepository, professionalRepository,
                weeklyScheduleRepository, syncVersionRepository);
    }
}