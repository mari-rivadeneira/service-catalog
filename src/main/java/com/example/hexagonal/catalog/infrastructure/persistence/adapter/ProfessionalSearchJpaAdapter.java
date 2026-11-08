package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalSearchRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arma la consulta con solo los filtros presentes: asi nunca se pasan parametros nulos a PostgreSQL
 * (por ejemplo lower(null)), que es una fuente conocida de errores de tipo.
 */
@Repository
public class ProfessionalSearchJpaAdapter implements ProfessionalSearchRepository {

    private static final String ENABLED_SCHEDULE_EXISTS =
            "exists (select 1 from WeeklyScheduleEntity s where s.professionalId = p.id and s.enabled = true";

    @PersistenceContext
    private EntityManager entityManager;

    private final ProfessionalMapper mapper;

    public ProfessionalSearchJpaAdapter(ProfessionalMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Professional> search(ProfessionalSearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (criteria.categoryId() != null) {
            where.append(" and p.categoryId = :categoryId");
            params.put("categoryId", criteria.categoryId());
        }
        if (criteria.name() != null) {
            where.append(" and (lower(p.firstName) like :name escape '\\'")
                    .append(" or lower(p.lastName) like :name escape '\\'")
                    .append(" or lower(concat(p.firstName, ' ', p.lastName)) like :name escape '\\')");
            params.put("name", likePattern(criteria.name()));
        }
        if (criteria.enabled() != null) {
            where.append(" and p.enabled = :enabled");
            params.put("enabled", criteria.enabled());
        }
        if (criteria.dayOfWeek() != null) {
            where.append(" and ").append(ENABLED_SCHEDULE_EXISTS).append(" and s.dayOfWeek = :dayOfWeek)");
            params.put("dayOfWeek", criteria.dayOfWeek());
        } else if (criteria.hasSchedules() != null) {
            where.append(criteria.hasSchedules() ? " and " : " and not ")
                    .append(ENABLED_SCHEDULE_EXISTS).append(")");
        }

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(p) from ProfessionalEntity p" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        TypedQuery<ProfessionalEntity> dataQuery = entityManager.createQuery(
                "select p from ProfessionalEntity p" + where
                        + " order by p.lastName asc, p.firstName asc, p.id asc", ProfessionalEntity.class);
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult(criteria.page() * criteria.size());
        dataQuery.setMaxResults(criteria.size());

        List<Professional> content = dataQuery.getResultList().stream().map(mapper::toDomain).toList();
        int totalPages = (int) ((total + criteria.size() - 1) / criteria.size());
        return new PageResult<>(content, criteria.page(), criteria.size(), total, totalPages);
    }

    /** El % y el _ que escriba el usuario se buscan como texto, no como comodines. */
    private static String likePattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
