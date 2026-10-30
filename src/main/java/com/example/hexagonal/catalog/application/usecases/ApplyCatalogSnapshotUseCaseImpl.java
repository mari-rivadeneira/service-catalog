package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ApplyCatalogSnapshotUseCaseImpl implements ApplyCatalogSnapshotUseCase {

    private static final String CATALOG_ENTITY_NAME = "CATALOG";

    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final SyncVersionRepository syncVersionRepository;

    public ApplyCatalogSnapshotUseCaseImpl(
            CategoryRepository categoryRepository,
            ProfessionalRepository professionalRepository,
            WeeklyScheduleRepository weeklyScheduleRepository,
            SyncVersionRepository syncVersionRepository) {
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.syncVersionRepository = syncVersionRepository;
    }

    @Override
    @Transactional
    public void applySnapshot(CatedraSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("El snapshot no puede ser nulo");
        }

        // 1. Guardar Categorías
        if (snapshot.getCategories() != null) {
            snapshot.getCategories().forEach(categoryRepository::save);
        }

        // 2. Guardar Profesionales
        if (snapshot.getProfessionals() != null) {
            snapshot.getProfessionals().forEach(professionalRepository::save);
        }

        // 3. Guardar Agendas Semanales
        if (snapshot.getSchedules() != null && !snapshot.getSchedules().isEmpty()) {
            weeklyScheduleRepository.saveAll(snapshot.getSchedules());
        }

        // 4. Actualizar o crear la versión en SyncVersion con los métodos exactos
        SyncVersion syncVersion = syncVersionRepository.findByEntityName(CATALOG_ENTITY_NAME)
                .orElseGet(() -> new SyncVersion(CATALOG_ENTITY_NAME, 0L, Instant.now()));

        syncVersion.setVersion(snapshot.getVersion());
        syncVersion.setLastSyncedAt(Instant.now());

        syncVersionRepository.save(syncVersion);
    }
}