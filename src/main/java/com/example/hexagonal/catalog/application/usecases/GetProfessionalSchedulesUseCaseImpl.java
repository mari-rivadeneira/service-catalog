package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.in.GetProfessionalSchedulesUseCase;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GetProfessionalSchedulesUseCaseImpl implements GetProfessionalSchedulesUseCase {

    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    public GetProfessionalSchedulesUseCaseImpl(ProfessionalRepository professionalRepository,
                                               WeeklyScheduleRepository weeklyScheduleRepository) {
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
    }

    @Override
    public List<WeeklySchedule> getSchedules(long professionalId) {
        // Un profesional inexistente es un 404; uno sin agendas devuelve una lista vacia
        professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado: " + professionalId));

        List<WeeklySchedule> schedules = new ArrayList<>(weeklyScheduleRepository.findByProfessionalId(professionalId));
        schedules.sort(Comparator.comparing(WeeklySchedule::getDayOfWeek)
                .thenComparing(WeeklySchedule::getStartTime));
        return schedules;
    }
}
