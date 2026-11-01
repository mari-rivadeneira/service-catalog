package com.example.hexagonal.catalog.infrastructure.redis.adapter;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.domain.model.CatalogChangeSet;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requiere Redis local (docker compose up -d). Usa un namespace de prueba para no tocar
 * nunca las claves reales catedra:sync:*.
 */
@SpringBootTest(properties = "catedra.redis.namespace=test:catedra:sync")
class CatedraRedisAdapterIntegrationTest {

    private static final String NS = "test:catedra:sync";

    @Autowired
    private CatedraRedisClient client;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    @AfterEach
    void cleanTestKeys() {
        Set<String> keys = redis.keys(NS + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private void putValue(String suffix, String value) {
        redis.opsForValue().set(NS + ":" + suffix, value);
    }

    private void putHash(String hash, String field, String json) {
        redis.opsForHash().put(NS + ":" + hash, field, json);
    }

    @Test
    @DisplayName("Lee current-version y oldest-available-version")
    void shouldReadVersions() {
        putValue("current-version", "7");
        putValue("oldest-available-version", "4");

        assertEquals(7L, client.getCurrentVersion());
        assertEquals(4L, client.getOldestAvailableVersion());
    }

    @Test
    @DisplayName("Si falta la version actual lanza CatalogHistoryUnavailableException")
    void shouldFailWhenVersionMissing() {
        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getCurrentVersion());
    }

    @Test
    @DisplayName("Si la version no es numerica lanza CatalogHistoryUnavailableException")
    void shouldFailWhenVersionIsNotANumber() {
        putValue("current-version", "siete");

        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getCurrentVersion());
    }

    @Test
    @DisplayName("Lee changes:{version} con el formato del contrato e ignora campos desconocidos")
    void shouldReadChanges() {
        putValue("changes:7", """
                {
                  "version": 7,
                  "campoNuevoCompatible": "ignorado",
                  "changes": {
                    "professionalCategories": [],
                    "professionals": [15],
                    "weeklySchedules": [21, 22]
                  }
                }
                """);

        CatalogChangeSet changes = client.getChanges(7);

        assertEquals(7L, changes.version());
        assertTrue(changes.professionalCategoryIds().isEmpty());
        assertEquals(1, changes.professionalIds().size());
        assertEquals(15L, changes.professionalIds().get(0));
        assertEquals(2, changes.weeklyScheduleIds().size());
    }

    @Test
    @DisplayName("Si falta changes:{version} lanza CatalogHistoryUnavailableException (discontinuidad)")
    void shouldFailWhenChangesMissing() {
        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getChanges(5));
    }

    @Test
    @DisplayName("Si changes:{version} trae otra version lanza CatalogHistoryUnavailableException")
    void shouldFailWhenChangesVersionDoesNotMatch() {
        putValue("changes:7", """
                {"version": 6, "changes": {"professionalCategories": [], "professionals": [], "weeklySchedules": []}}
                """);

        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getChanges(7));
    }

    @Test
    @DisplayName("Lee una categoria deshabilitada (baja logica) desde el hash")
    void shouldReadDisabledCategory() {
        putHash("professional-categories", "1", """
                {
                  "id": 1,
                  "name": "Clinica medica",
                  "description": "Atencion clinica general",
                  "enabled": false,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
                """);

        ProfessionalCategory category = client.getCategory(1);

        assertEquals(1L, category.getId());
        assertEquals("Clinica medica", category.getName());
        assertFalse(category.isEnabled());
    }

    @Test
    @DisplayName("Lee un profesional desde el hash")
    void shouldReadProfessional() {
        putHash("professionals", "15", """
                {
                  "id": 15,
                  "categoryId": 1,
                  "firstName": "Ana",
                  "lastName": "Gomez",
                  "enabled": true,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
                """);

        Professional professional = client.getProfessional(15);

        assertEquals(15L, professional.getId());
        assertEquals(1L, professional.getCategoryId());
        assertEquals("Gomez", professional.getLastName());
        assertTrue(professional.isEnabled());
    }

    @Test
    @DisplayName("Lee agendas con horas HH:mm:ss y HH:mm")
    void shouldReadSchedulesWithBothTimeFormats() {
        putHash("weekly-schedules", "21", """
                {
                  "id": 21, "professionalId": 15, "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00", "endTime": "12:00:00", "slotDurationMinutes": 30,
                  "enabled": true, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"
                }
                """);
        putHash("weekly-schedules", "22", """
                {
                  "id": 22, "professionalId": 15, "dayOfWeek": "TUESDAY",
                  "startTime": "09:00", "endTime": "12:00", "slotDurationMinutes": 30,
                  "enabled": true, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"
                }
                """);

        WeeklySchedule withSeconds = client.getWeeklySchedule(21);
        WeeklySchedule withoutSeconds = client.getWeeklySchedule(22);

        assertEquals(DayOfWeek.MONDAY, withSeconds.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), withSeconds.getStartTime());
        assertEquals(DayOfWeek.TUESDAY, withoutSeconds.getDayOfWeek());
        assertEquals(LocalTime.of(12, 0), withoutSeconds.getEndTime());
    }

    @Test
    @DisplayName("Si el id no esta en el hash lanza CatalogHistoryUnavailableException")
    void shouldFailWhenHashFieldMissing() {
        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getProfessional(999));
    }

    @Test
    @DisplayName("Si el JSON del hash esta corrupto lanza CatalogHistoryUnavailableException")
    void shouldFailWhenHashContentIsInvalid() {
        putHash("professionals", "15", "esto no es json");

        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getProfessional(15));
    }

    @Test
    @DisplayName("Si el id del contenido no coincide con el field lanza CatalogHistoryUnavailableException")
    void shouldFailWhenEntityIdDoesNotMatchField() {
        putHash("professionals", "15", """
                {"id": 99, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true,
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
                """);

        assertThrows(CatalogHistoryUnavailableException.class, () -> client.getProfessional(15));
    }
}