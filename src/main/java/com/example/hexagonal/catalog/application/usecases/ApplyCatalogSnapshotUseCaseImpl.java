package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidCatedraSnapshotException;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplyCatalogSnapshotUseCaseImpl implements ApplyCatalogSnapshotUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyCatalogSnapshotUseCaseImpl.class);

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

        // Bloquea la fila de estado: serializa este snapshot con cualquier sincronizacion incremental en curso
        SyncVersion state = syncVersionRepository.findByEntityNameForUpdate(SyncVersion.CATALOG_KEY)
                .orElseGet(SyncVersion::initial);

        // Reemplazo completo de la copia local: borrado en orden inverso de FK
        weeklyScheduleRepository.deleteAll();
        professionalRepository.deleteAll();
        categoryRepository.deleteAll();

        // Reinsercion en orden de FK
        categoryRepository.saveAll(snapshot.getCategories());
        professionalRepository.saveAll(snapshot.getProfessionals());
        weeklyScheduleRepository.saveAll(snapshot.getSchedules());

        // Se registra exactamente la version del snapshot aplicado, en la misma transaccion
        state.rebuiltFromSnapshot(snapshot.getVersion());
        syncVersionRepository.save(state);

        log.info("Snapshot aplicado: version {} ({} categorias, {} profesionales, {} agendas)",
                snapshot.getVersion(), snapshot.getCategories().size(), snapshot.getProfessionals().size(),
                snapshot.getSchedules().size());
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