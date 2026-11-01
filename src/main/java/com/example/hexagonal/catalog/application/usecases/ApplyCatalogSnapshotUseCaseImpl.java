package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidCatedraSnapshotException;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplyCatalogSnapshotUseCaseImpl implements ApplyCatalogSnapshotUseCase {

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
        validate(snapshot);

        // Reemplazo completo de la copia local: borrado en orden inverso de FK
        weeklyScheduleRepository.deleteAll();
        professionalRepository.deleteAll();
        categoryRepository.deleteAll();

        // Reinsercion en orden de FK
        categoryRepository.saveAll(snapshot.getCategories());
        professionalRepository.saveAll(snapshot.getProfessionals());
        weeklyScheduleRepository.saveAll(snapshot.getSchedules());

        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY)
                .orElseGet(SyncVersion::initial);
        state.rebuiltFromSnapshot(snapshot.getVersion());
        syncVersionRepository.save(state);
    }

    private void validate(CatedraSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("El snapshot no puede ser nulo");
        }
        if (snapshot.getCategories() == null
                || snapshot.getProfessionals() == null
                || snapshot.getSchedules() == null) {
            throw new InvalidCatedraSnapshotException("El snapshot debe incluir las tres colecciones");
        }
    }
}