package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.List;

@Component
public class CatedraSnapshotMapper {

    public CatedraSnapshot toDomain(CatedraSnapshotResponseDto dto) {
        if (dto == null) {
            return null;
        }

        List<ProfessionalCategory> categories = dto.categories() != null
                ? dto.categories().stream()
                .map(c -> new ProfessionalCategory(c.id(), c.name(), c.description(), c.createdAt(), c.updatedAt()))
                .toList()
                : Collections.emptyList();

        List<Professional> professionals = dto.professionals() != null
                ? dto.professionals().stream()
                .map(p -> new Professional(p.id(), p.firstName(), p.lastName(), p.email(), p.phone(),
                        p.categoryId(), p.active(), p.createdAt(), p.updatedAt()))
                .toList()
                : Collections.emptyList();

        List<WeeklySchedule> schedules = dto.schedules() != null
                ? dto.schedules().stream()
                .map(s -> new WeeklySchedule(s.id(), s.professionalId(),
                        DayOfWeek.valueOf(s.dayOfWeek().toUpperCase()),
                        s.startTime(), s.endTime(), s.slotDurationMinutes()))
                .toList()
                : Collections.emptyList();

        return new CatedraSnapshot(dto.version(), categories, professionals, schedules);
    }
}