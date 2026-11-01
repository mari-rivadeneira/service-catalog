package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataWeeklyScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WeeklyScheduleJpaAdapter implements WeeklyScheduleRepository {

    private final SpringDataWeeklyScheduleRepository repository;
    private final WeeklyScheduleMapper mapper;

    public WeeklyScheduleJpaAdapter(SpringDataWeeklyScheduleRepository repository, WeeklyScheduleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public WeeklySchedule save(WeeklySchedule weeklySchedule) {
        return mapper.toDomain(repository.save(mapper.toEntity(weeklySchedule)));
    }

    @Override
    public List<WeeklySchedule> saveAll(List<WeeklySchedule> schedules) {
        return repository.saveAll(schedules.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WeeklySchedule> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WeeklySchedule> findByProfessionalId(Long professionalId) {
        return repository.findByProfessionalId(professionalId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}