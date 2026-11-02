package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.application.exception.InvalidCatedraSnapshotException;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.CategoryDto;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.ProfessionalDto;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.WeeklyScheduleDto;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;

@Component
public class CatedraSnapshotMapper {

    public CatedraSnapshot toDomain(CatedraSnapshotResponseDto dto) {
        if (dto == null) {
            throw new InvalidCatedraSnapshotException("El snapshot de catedra es nulo");
        }
        require(dto.snapshotVersion(), "snapshotVersion");
        if (dto.professionalCategories() == null
                || dto.professionals() == null
                || dto.weeklySchedules() == null) {
            throw new InvalidCatedraSnapshotException("El snapshot debe incluir las tres colecciones");
        }

        List<ProfessionalCategory> categories = dto.professionalCategories().stream()
                .map(this::toCategory).toList();
        List<Professional> professionals = dto.professionals().stream()
                .map(this::toProfessional).toList();
        List<WeeklySchedule> schedules = dto.weeklySchedules().stream()
                .map(this::toSchedule).toList();

        return new CatedraSnapshot(dto.snapshotVersion(), categories, professionals, schedules);
    }

    public ProfessionalCategory toCategory(CategoryDto c) {
        return new ProfessionalCategory(require(c.id(), "professionalCategories.id"), c.name(),
                c.description(), require(c.enabled(), "professionalCategories.enabled"),
                c.createdAt(), c.updatedAt());
    }

    public Professional toProfessional(ProfessionalDto p) {
        return new Professional(require(p.id(), "professionals.id"),
                require(p.categoryId(), "professionals.categoryId"), p.firstName(), p.lastName(),
                require(p.enabled(), "professionals.enabled"), p.createdAt(), p.updatedAt());
    }

    public WeeklySchedule toSchedule(WeeklyScheduleDto s) {
        return new WeeklySchedule(require(s.id(), "weeklySchedules.id"),
                require(s.professionalId(), "weeklySchedules.professionalId"),
                parseDay(s.dayOfWeek()), s.startTime(), s.endTime(),
                require(s.slotDurationMinutes(), "weeklySchedules.slotDurationMinutes"),
                require(s.enabled(), "weeklySchedules.enabled"), s.createdAt(), s.updatedAt());
    }

    private static DayOfWeek parseDay(String value) {
        require(value, "weeklySchedules.dayOfWeek");
        try {
            return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCatedraSnapshotException("Dia de la semana invalido en el snapshot: " + value);
        }
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new InvalidCatedraSnapshotException("Campo obligatorio ausente en el snapshot: " + field);
        }
        return value;
    }
}