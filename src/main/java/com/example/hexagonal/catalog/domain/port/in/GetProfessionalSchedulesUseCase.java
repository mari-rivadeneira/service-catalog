package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;

import java.util.List;

public interface GetProfessionalSchedulesUseCase {

    /**
     * Agendas semanales del profesional (habilitadas y deshabilitadas), ordenadas por dia y hora de inicio.
     *
     * @throws com.example.hexagonal.catalog.application.exception.EntityNotFoundException si el profesional no existe
     */
    List<WeeklySchedule> getSchedules(long professionalId);
}
