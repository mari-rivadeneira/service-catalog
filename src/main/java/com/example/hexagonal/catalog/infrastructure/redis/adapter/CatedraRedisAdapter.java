package com.example.hexagonal.catalog.infrastructure.redis.adapter;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.application.exception.CatedraRedisException;
import com.example.hexagonal.catalog.application.exception.InvalidCatedraSnapshotException;
import com.example.hexagonal.catalog.domain.model.CatalogChangeSet;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import com.example.hexagonal.catalog.infrastructure.client.catedra.CatedraSnapshotMapper;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.CategoryDto;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.ProfessionalDto;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto.WeeklyScheduleDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

@Component
public class CatedraRedisAdapter implements CatedraRedisClient {

    private static final String CURRENT_VERSION = "current-version";
    private static final String OLDEST_AVAILABLE_VERSION = "oldest-available-version";
    private static final String CHANGES_PREFIX = "changes:";
    private static final String CATEGORIES_HASH = "professional-categories";
    private static final String PROFESSIONALS_HASH = "professionals";
    private static final String SCHEDULES_HASH = "weekly-schedules";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CatedraSnapshotMapper mapper;
    private final String namespace;

    public CatedraRedisAdapter(StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               CatedraSnapshotMapper mapper,
                               @Value("${catedra.redis.namespace:catedra:sync}") String namespace) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.namespace = namespace;
    }

    @Override
    public long getCurrentVersion() {
        return readVersion(key(CURRENT_VERSION));
    }

    @Override
    public long getOldestAvailableVersion() {
        return readVersion(key(OLDEST_AVAILABLE_VERSION));
    }

    @Override
    public CatalogChangeSet getChanges(long version) {
        String key = key(CHANGES_PREFIX + version);
        String raw = execute(() -> redisTemplate.opsForValue().get(key));
        if (raw == null) {
            throw new CatalogHistoryUnavailableException(
                    "No existe el historial de la version " + version + " (" + key + ")");
        }

        ChangesDto dto;
        try {
            dto = objectMapper.readValue(raw, ChangesDto.class);
        } catch (JacksonException e) {
            throw new CatalogHistoryUnavailableException("Contenido invalido en " + key, e);
        }

        if (dto.version() == null || dto.version() != version || dto.changes() == null
                || dto.changes().professionalCategories() == null
                || dto.changes().professionals() == null
                || dto.changes().weeklySchedules() == null) {
            throw new CatalogHistoryUnavailableException("Contenido inconsistente en " + key);
        }

        return new CatalogChangeSet(version,
                dto.changes().professionalCategories(),
                dto.changes().professionals(),
                dto.changes().weeklySchedules());
    }

    @Override
    public ProfessionalCategory getCategory(long id) {
        return readEntity(CATEGORIES_HASH, id, CategoryDto.class, mapper::toCategory,
                ProfessionalCategory::getId);
    }

    @Override
    public Professional getProfessional(long id) {
        return readEntity(PROFESSIONALS_HASH, id, ProfessionalDto.class, mapper::toProfessional,
                Professional::getId);
    }

    @Override
    public WeeklySchedule getWeeklySchedule(long id) {
        return readEntity(SCHEDULES_HASH, id, WeeklyScheduleDto.class, mapper::toSchedule,
                WeeklySchedule::getId);
    }

    private long readVersion(String key) {
        String raw = execute(() -> redisTemplate.opsForValue().get(key));
        if (raw == null) {
            throw new CatalogHistoryUnavailableException("No existe la clave " + key);
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new CatalogHistoryUnavailableException("Version invalida en " + key + ": " + raw, e);
        }
    }

    private <D, T> T readEntity(String hash, long id, Class<D> dtoType,
                                Function<D, T> converter, ToLongFunction<T> idOf) {
        String hashKey = key(hash);
        String field = Long.toString(id);
        String raw = execute(() -> redisTemplate.<String, String>opsForHash().get(hashKey, field));
        if (raw == null) {
            throw new CatalogHistoryUnavailableException(
                    "No existe el id " + id + " en el hash " + hashKey);
        }

        T entity;
        try {
            entity = converter.apply(objectMapper.readValue(raw, dtoType));
        } catch (JacksonException | InvalidCatedraSnapshotException e) {
            throw new CatalogHistoryUnavailableException(
                    "Contenido invalido para el id " + id + " en " + hashKey, e);
        }

        if (idOf.applyAsLong(entity) != id) {
            throw new CatalogHistoryUnavailableException(
                    "El id " + id + " en " + hashKey + " contiene una entidad con otro id");
        }
        return entity;
    }

    private String key(String suffix) {
        return namespace + ":" + suffix;
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException e) {
            throw new CatedraRedisException("No se pudo leer de Redis de catedra", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChangesDto(Long version, ChangesBody changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChangesBody(List<Long> professionalCategories, List<Long> professionals, List<Long> weeklySchedules) {
    }
}