# service-catalog

Servicio de catálogo y sincronización del proyecto integrador 2026 (sistema distribuido de turnos).
Autora: Rivadeneira Marina.

Mantiene una copia local del catálogo de la cátedra (categorías, profesionales y horarios semanales), la sincroniza
de forma completa (snapshot por REST) e incremental (Kafka y Redis), permite consultarla con filtros y registra y
autentica a los usuarios finales con JWT. Es un caso de estudio académico con datos ficticios.

## Requisitos
- Java 21 y Docker con Docker Compose.
- openssl, para generar el secreto del JWT.
- Para sincronizar con la cátedra: acceso a su red (ZeroTier) y la cuenta técnica del proyecto, que se registra una
  vez con Postman: `POST /api/student/register` contra la URL base que informa la cátedra. La respuesta trae el JWT
  técnico y la configuración de Redis y Kafka.

## Configuración
1. Copiar el archivo de ejemplo: `cp .env.example .env`
2. Completar `JWT_SECRET` (obligatoria): `openssl rand -base64 64 | tr -d '\n'`. service-turns usa el mismo valor.
3. Para sincronizar, completar las variables `CATEDRA_*` y las credenciales de Redis con lo que devolvió el registro
   de la cuenta técnica.

| Variable | Obligatoria | Descripción |
|---|---|---|
| JWT_SECRET | Sí | Secreto Base64 (mínimo 64 bytes) con el que se firman y validan los JWT |
| CATEDRA_API_BASE_URL, CATEDRA_JWT_TOKEN | Para sincronizar | URL de la API de la cátedra y JWT técnico |
| CATEDRA_REDIS_HOST, CATEDRA_REDIS_PORT, SPRING_DATA_REDIS_USERNAME, SPRING_DATA_REDIS_PASSWORD | Para sincronizar | Redis de la cátedra |
| CATEDRA_KAFKA_BOOTSTRAP_SERVERS, CATEDRA_KAFKA_CONSUMER_GROUP_ID, CATEDRA_KAFKA_CATALOG_TOPIC | Para sincronizar | Kafka de la cátedra |
| POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_PORT, REDIS_PORT | No | Infraestructura local del compose |
| CATALOG_RECONCILIATION_ENABLED, CATALOG_RECONCILIATION_INTERVAL_MS | No | Reconciliación al arrancar y periódica |
| CORS_ALLOWED_ORIGINS | No | Orígenes CORS permitidos (vacío: ninguno) |
| PASSWORD_ARGON2_MEMORY_KIB, PASSWORD_ARGON2_ITERATIONS, PASSWORD_ARGON2_PARALLELISM | No | Costo del hash de contraseñas |

## Ejecución con Docker
```bash
docker compose --profile app up -d --build
```
Levanta PostgreSQL y el servicio, que arranca cuando la base está lista y escucha en `http://localhost:8080`. El
servicio se conecta a REST, Redis y Kafka de la cátedra con lo definido en `.env`. Para comprobar que está vivo:
`curl -i http://localhost:8080/api/account` responde 401.

| Acción | Comando |
|---|---|
| Ver estado | `docker compose ps` |
| Ver logs del servicio | `docker compose logs -f catalog-service` |
| Detener (conserva los datos) | `docker compose --profile app down` |
| Borrar todo, incluidos los datos | `docker compose --profile app down -v` |

## Ejecución desde el IDE o con Maven
`docker compose up -d` levanta PostgreSQL, Redis y Kafka locales, sin el servicio. Después:
```bash
JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')" ./mvnw spring-boot:run
```
Sin las variables de la cátedra el servicio funciona, pero no sincroniza. Para sincronizar, agregar a la ejecución las
variables `CATEDRA_*` y `CATALOG_RECONCILIATION_ENABLED=true`. No se carga el `.env` completo en la terminal: las
variables vacías pisan los valores por defecto.

## Pruebas
```bash
docker compose up -d
./mvnw clean test
```
Las pruebas de integración usan PostgreSQL, Redis y Kafka del compose. La terminal no debe tener definidas las
variables de la cátedra ni `JWT_SECRET`: las pruebas usan su propia configuración. Reescriben las tablas del catálogo
de la base local.

## API
Todas las rutas exigen `Authorization: Bearer <id_token>`, salvo el registro y el ingreso.

| Método y ruta | Descripción |
|---|---|
| POST /api/register | Registra un usuario (login, password, firstName, lastName, email, imageUrl opcional, langKey) |
| POST /api/authenticate | Ingreso por login o email: devuelve `id_token` |
| GET /api/account | Cuenta del usuario autenticado |
| GET /api/catalog/professionals | Búsqueda: categoryId, name, enabled, hasSchedules, dayOfWeek, page, size |
| GET /api/catalog/professionals/{id} | Un profesional |
| GET /api/catalog/professionals/{id}/schedules | Agendas semanales del profesional |
| GET /api/catalog/categories | Categorías |
| GET /api/catalog/sync/status | Estado de la sincronización |

```bash
curl -X POST http://localhost:8080/api/register -H "Content-Type: application/json" \
  -d '{"login":"ana","password":"clave-segura","email":"ana@example.com","firstName":"Ana","lastName":"Perez"}'
curl -X POST http://localhost:8080/api/authenticate -H "Content-Type: application/json" \
  -d '{"username":"ana","password":"clave-segura"}'
curl "http://localhost:8080/api/catalog/professionals?hasSchedules=true&dayOfWeek=monday&size=5" \
  -H "Authorization: Bearer <id_token>"
```
Los errores salen en `application/problem+json` con un `code` estable.

## Estructura
```
src/main/java/.../catalog
  domain/model, domain/port/in, domain/port/out   modelos y puertos
  application/usecases, service, exception         casos de uso
  infrastructure/persistence, client/catedra,      adaptadores: JPA, REST, Redis, Kafka,
    redis, kafka, scheduling, security, web, config seguridad y API
src/main/resources/db/migration                    migraciones de Flyway
```

## Seguridad
El servicio expone HTTP, según lo acordado con la cátedra. Los usuarios se autentican con JWT firmado con HS512 y las
contraseñas se guardan con Argon2id. CORS no permite ningún origen por defecto.

## Documentación
Las decisiones de arquitectura (ADR), el modelo de datos, los diagramas, los contratos y las evidencias están en el
repositorio de documentación: https://github.com/mari-rivadeneira/project-docs