package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataProfessionalRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataWeeklyScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WeeklyScheduleJpaAdapter implements WeeklyScheduleRepository {

    private final SpringDataWeeklyScheduleRepository scheduleRepo;
    private final SpringDataProfessionalRepository professionalRepo;
    private final WeeklyScheduleMapper mapper;

    public WeeklyScheduleJpaAdapter(SpringDataWeeklyScheduleRepository scheduleRepo,
                                    SpringDataProfessionalRepository professionalRepo,
                                    WeeklyScheduleMapper mapper) {
        this.scheduleRepo = scheduleRepo;
        this.professionalRepo = professionalRepo;
        this.mapper = mapper;
    }

    @Override
    public WeeklySchedule save(WeeklySchedule weeklySchedule) {
        ProfessionalEntity professional = professionalRepo.findById(weeklySchedule.getProfessionalId())
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado"));
        WeeklyScheduleEntity entity = mapper.toEntity(weeklySchedule, professional);
        return mapper.toDomain(scheduleRepo.save(entity));
    }

    @Override
    public List<WeeklySchedule> saveAll(List<WeeklySchedule> schedules) {
        return schedules.stream().map(this::save).toList();
    }

    @Override
    public Optional<WeeklySchedule> findById(UUID id) {
        return scheduleRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WeeklySchedule> findByProfessionalId(UUID professionalId) {
        return scheduleRepo.findByProfessionalId(professionalId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByProfessionalId(UUID professionalId) {
        scheduleRepo.deleteByProfessionalId(professionalId);
    }
}