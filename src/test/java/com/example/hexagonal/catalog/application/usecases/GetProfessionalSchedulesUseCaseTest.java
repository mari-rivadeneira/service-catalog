package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfessionalSchedulesUseCaseTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;

    private GetProfessionalSchedulesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfessionalSchedulesUseCaseImpl(professionalRepository, weeklyScheduleRepository);
    }

    private WeeklySchedule schedule(long id, DayOfWeek day, int startHour) {
        Instant now = Instant.now();
        return new WeeklySchedule(id, 15L, day, LocalTime.of(startHour, 0), LocalTime.of(startHour + 3, 0),
                30, true, now, now);
    }

    @Test
    @DisplayName("Devuelve las agendas ordenadas por dia de la semana y hora de inicio")
    void shouldReturnSchedulesSortedByDayAndStartTime() {
        when(professionalRepository.findById(15L)).thenReturn(Optional.of(new Professional()));
        when(weeklyScheduleRepository.findByProfessionalId(15L)).thenReturn(List.of(
                schedule(1L, DayOfWeek.FRIDAY, 9),
                schedule(2L, DayOfWeek.MONDAY, 14),
                schedule(3L, DayOfWeek.MONDAY, 9)));

        List<WeeklySchedule> result = useCase.getSchedules(15L);

        assertEquals(List.of(3L, 2L, 1L), result.stream().map(WeeklySchedule::getId).toList());
    }

    @Test
    @DisplayName("Si el profesional no existe lanza EntityNotFoundException y no busca agendas")
    void shouldFailWhenProfessionalDoesNotExist() {
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.getSchedules(99L));

        verifyNoInteractions(weeklyScheduleRepository);
    }
}
