package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.domain.model.CatalogDelta;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogDeltaUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplyCatalogDeltaUseCaseImpl implements ApplyCatalogDeltaUseCase {

    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final SyncVersionRepository syncVersionRepository;

    public ApplyCatalogDeltaUseCaseImpl(
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
    public void applyDelta(CatalogDelta delta) {
        // El bloqueo serializa esta version con cualquier otro incremental o snapshot en curso
        SyncVersion state = syncVersionRepository.findByEntityNameForUpdate(SyncVersion.CATALOG_KEY)
                .orElseGet(SyncVersion::initial);

        if (state.getLastMode() == null) {
            throw new CatalogHistoryUnavailableException(
                    "No hay copia local sobre la cual aplicar la version " + delta.version());
        }

        long current = state.getVersion();
        if (current >= delta.version()) {
            return;
        }
        if (current != delta.version() - 1) {
            throw new CatalogHistoryUnavailableException(
                    "Hueco de versiones: local=" + current + ", se intento aplicar " + delta.version());
        }

        verifyReferences(delta);

        // Upsert en orden de FK. Las entidades deshabilitadas se conservan.
        categoryRepository.saveAll(delta.categories());
        professionalRepository.saveAll(delta.professionals());
        weeklyScheduleRepository.saveAll(delta.schedules());

        state.applied(delta.version(), SyncMode.INCREMENTAL);
        syncVersionRepository.save(state);
    }

    // Una referencia imposible de resolver significa que el historial no es confiable: hay que usar snapshot.
    private void verifyReferences(CatalogDelta delta) {
        Set<Long> categoryIds = delta.categories().stream()
                .map(ProfessionalCategory::getId).collect(Collectors.toSet());
        for (Professional professional : delta.professionals()) {
            if (!categoryIds.contains(professional.getCategoryId())
                    && categoryRepository.findById(professional.getCategoryId()).isEmpty()) {
                throw new CatalogHistoryUnavailableException("El profesional " + professional.getId()
                        + " referencia la categoria inexistente " + professional.getCategoryId());
            }
        }

        Set<Long> professionalIds = delta.professionals().stream()
                .map(Professional::getId).collect(Collectors.toSet());
        for (WeeklySchedule schedule : delta.schedules()) {
            if (!professionalIds.contains(schedule.getProfessionalId())
                    && professionalRepository.findById(schedule.getProfessionalId()).isEmpty()) {
                throw new CatalogHistoryUnavailableException("La agenda " + schedule.getId()
                        + " referencia el profesional inexistente " + schedule.getProfessionalId());
            }
        }
    }
}
