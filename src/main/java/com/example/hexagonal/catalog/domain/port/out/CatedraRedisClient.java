package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.CatalogChangeSet;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;


 // Lectura del catalogo publicado por la catedra en Redis (solo lectura).
 //Todos los metodos lanzan CatalogHistoryUnavailableException si falta la clave o el id,
 //o si el contenido no se puede interpretar, y CatedraRedisException si Redis no responde.

public interface CatedraRedisClient {

    long getCurrentVersion();

    long getOldestAvailableVersion();

    CatalogChangeSet getChanges(long version);

    ProfessionalCategory getCategory(long id);

    Professional getProfessional(long id);

    WeeklySchedule getWeeklySchedule(long id);
}