package com.example.hexagonal.catalog.infrastructure.web;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requiere el compose activo. Los datos de prueba usan ids altos y las busquedas filtran por la categoria de prueba,
 * asi que no se mezclan con un catalogo real. Cada test se deshace al terminar (@Transactional).
 */
@SpringBootTest
@Transactional
class CatalogApiIntegrationTest {

    private static final long C1 = 920_001L;
    private static final long C2 = 920_002L;
    private static final long P1 = 920_101L; // Ana Gomez: habilitada, agendas: lunes 14, lunes 9 y viernes 9 (deshabilitada)
    private static final long P2 = 920_102L; // Luis Perez: habilitado, sin agendas
    private static final long P3 = 920_103L; // Ana Lopez: deshabilitada, agenda habilitada el lunes
    private static final long P4 = 920_104L; // Marta Diaz: categoria 2
    private static final long S_MON_AFTERNOON = 920_201L;
    private static final long S_MON_MORNING = 920_202L;
    private static final long S_FRI_DISABLED = 920_203L;
    private static final long S_P3 = 920_204L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private SyncVersionRepository syncVersionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("tester")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("auth", "ROLE_USER")
                .claim("userId", 1L)
                .build();
        token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims))
                .getTokenValue();

        categoryRepository.saveAll(List.of(
                new ProfessionalCategory(C1, "Test api A", null, true, now, now),
                new ProfessionalCategory(C2, "Test api B", null, false, now, now)));
        professionalRepository.saveAll(List.of(
                new Professional(P1, C1, "Ana", "Gomez", true, now, now),
                new Professional(P2, C1, "Luis", "Perez", true, now, now),
                new Professional(P3, C1, "Ana", "Lopez", false, now, now),
                new Professional(P4, C2, "Marta", "Diaz", true, now, now)));
        weeklyScheduleRepository.saveAll(List.of(
                schedule(S_MON_AFTERNOON, P1, DayOfWeek.MONDAY, 14, true),
                schedule(S_MON_MORNING, P1, DayOfWeek.MONDAY, 9, true),
                schedule(S_FRI_DISABLED, P1, DayOfWeek.FRIDAY, 9, false),
                schedule(S_P3, P3, DayOfWeek.MONDAY, 9, true)));
        entityManager.flush();
        entityManager.clear();
    }

    // ---------- helpers ----------

    private WeeklySchedule schedule(long id, long professionalId, DayOfWeek day, int startHour, boolean enabled) {
        Instant now = Instant.now();
        return new WeeklySchedule(id, professionalId, day, LocalTime.of(startHour, 0),
                LocalTime.of(startHour + 3, 0), 30, enabled, now, now);
    }

    private ResultActions getWithToken(String path) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private List<Long> longs(String json, String path) {
        List<Number> raw = JsonPath.read(json, path);
        return raw.stream().map(Number::longValue).toList();
    }

    /** Busca dentro de la categoria de prueba; extra empieza con "&". */
    private List<Long> searchIds(String extra) throws Exception {
        String json = body(getWithToken("/api/catalog/professionals?categoryId=" + C1 + extra)
                .andExpect(status().isOk()));
        return longs(json, "$.content[*].id");
    }

    // ---------- autenticacion ----------

    @Test
    @DisplayName("Sin token, todos los endpoints del catalogo responden 401")
    void shouldRejectRequestsWithoutToken() throws Exception {
        for (String path : List.of("/api/catalog/professionals", "/api/catalog/professionals/" + P1,
                "/api/catalog/professionals/" + P1 + "/schedules", "/api/catalog/categories",
                "/api/catalog/sync/status")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    // ---------- busqueda y filtros ----------

    @Test
    @DisplayName("Busqueda por categoria: forma de la respuesta, orden por apellido y datos de paginacion")
    void shouldSearchByCategoryWithExpectedShape() throws Exception {
        getWithToken("/api/catalog/professionals?categoryId=" + C1)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].id").value(P1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ana"))
                .andExpect(jsonPath("$.content[0].lastName").value("Gomez"))
                .andExpect(jsonPath("$.content[0].categoryId").value(C1))
                .andExpect(jsonPath("$.content[0].enabled").value(true));

        assertEquals(List.of(P1, P3, P2), searchIds(""));
    }

    @Test
    @DisplayName("Filtro por nombre: parcial, sin distinguir mayusculas, y por nombre completo")
    void shouldFilterByName() throws Exception {
        assertEquals(List.of(P1, P3), searchIds("&name=ANA"));
        // MockMvc codifica la URL: se escribe el espacio y el % sin codificar, si no se codificarian dos veces
        assertEquals(List.of(P1), searchIds("&name=ana gomez"));
        assertTrue(searchIds("&name=%").isEmpty());
    }

    @Test
    @DisplayName("Filtro por estado habilitado")
    void shouldFilterByEnabled() throws Exception {
        assertEquals(List.of(P1, P2), searchIds("&enabled=true"));
        assertEquals(List.of(P3), searchIds("&enabled=false"));
    }

    @Test
    @DisplayName("Filtro de disponibilidad: con y sin agenda habilitada")
    void shouldFilterByAvailability() throws Exception {
        assertEquals(List.of(P1, P3), searchIds("&hasSchedules=true"));
        assertEquals(List.of(P2), searchIds("&hasSchedules=false"));
    }

    @Test
    @DisplayName("Filtro por dia de la semana, en cualquier combinacion de mayusculas; una agenda deshabilitada no cuenta")
    void shouldFilterByDayOfWeek() throws Exception {
        assertEquals(List.of(P1, P3), searchIds("&dayOfWeek=monday"));
        assertEquals(List.of(P1, P3), searchIds("&dayOfWeek=MONDAY"));
        assertTrue(searchIds("&dayOfWeek=friday").isEmpty());
    }

    @Test
    @DisplayName("Los filtros se combinan")
    void shouldCombineFilters() throws Exception {
        assertEquals(List.of(P1), searchIds("&name=ana&enabled=true&hasSchedules=true"));
        assertEquals(List.of(P3), searchIds("&name=ana&enabled=false&dayOfWeek=monday"));
    }

    @Test
    @DisplayName("Paginacion: paginas, total y cantidad de paginas")
    void shouldPaginate() throws Exception {
        String first = body(getWithToken("/api/catalog/professionals?categoryId=" + C1 + "&page=0&size=2")
                .andExpect(status().isOk()));
        assertEquals(List.of(P1, P3), longs(first, "$.content[*].id"));
        assertEquals(3, ((Number) JsonPath.read(first, "$.totalElements")).intValue());
        assertEquals(2, ((Number) JsonPath.read(first, "$.totalPages")).intValue());

        assertEquals(List.of(P2), searchIds("&page=1&size=2"));
        assertTrue(searchIds("&page=2&size=2").isEmpty());
    }

    @Test
    @DisplayName("Criterios invalidos: 400 VALIDATION_ERROR con el detalle por campo")
    void shouldRejectInvalidCriteria() throws Exception {
        getWithToken("/api/catalog/professionals?size=0")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        getWithToken("/api/catalog/professionals?size=101")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        getWithToken("/api/catalog/professionals?page=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").exists());

        getWithToken("/api/catalog/professionals?name=" + "x".repeat(101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        getWithToken("/api/catalog/professionals?dayOfWeek=funday")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dayOfWeek").exists());

        getWithToken("/api/catalog/professionals?dayOfWeek=monday&hasSchedules=false")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dayOfWeek").exists());
    }

    @Test
    @DisplayName("Valores con formato incorrecto: 400 BAD_REQUEST")
    void shouldRejectWrongFormats() throws Exception {
        for (String query : List.of("enabled=quizas", "page=abc", "categoryId=abc", "hasSchedules=tal_vez")) {
            getWithToken("/api/catalog/professionals?" + query)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    // ---------- profesional por id ----------

    @Test
    @DisplayName("Devuelve un profesional por id, incluso deshabilitado")
    void shouldReturnProfessionalById() throws Exception {
        getWithToken("/api/catalog/professionals/" + P1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(P1))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.enabled").value(true));

        getWithToken("/api/catalog/professionals/" + P3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("Profesional inexistente: 404 NOT_FOUND; id que no es numero: 400 BAD_REQUEST")
    void shouldReturnNotFoundForUnknownProfessional() throws Exception {
        getWithToken("/api/catalog/professionals/999999999")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        getWithToken("/api/catalog/professionals/abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ---------- agendas ----------

    @Test
    @DisplayName("Agendas del profesional: ordenadas por dia y hora, con las deshabilitadas marcadas")
    void shouldReturnSchedulesSortedAndIncludingDisabled() throws Exception {
        String json = body(getWithToken("/api/catalog/professionals/" + P1 + "/schedules")
                .andExpect(status().isOk()));

        assertEquals(List.of(S_MON_MORNING, S_MON_AFTERNOON, S_FRI_DISABLED), longs(json, "$[*].id"));
        assertEquals("MONDAY", JsonPath.read(json, "$[0].dayOfWeek"));
        assertTrue(((String) JsonPath.read(json, "$[0].startTime")).startsWith("09:00"));
        assertEquals(30, ((Number) JsonPath.read(json, "$[0].slotDurationMinutes")).intValue());
        assertEquals(true, JsonPath.read(json, "$[0].enabled"));
        assertEquals(false, JsonPath.read(json, "$[2].enabled"));
    }

    @Test
    @DisplayName("Un profesional sin agendas devuelve una lista vacia; uno inexistente, 404")
    void shouldReturnEmptyListOrNotFoundForSchedules() throws Exception {
        getWithToken("/api/catalog/professionals/" + P2 + "/schedules")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        getWithToken("/api/catalog/professionals/999999999/schedules")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---------- categorias ----------

    @Test
    @DisplayName("Categorias: ordenadas por nombre, con las deshabilitadas marcadas")
    void shouldListCategoriesSortedByName() throws Exception {
        String json = body(getWithToken("/api/catalog/categories").andExpect(status().isOk()));

        List<String> names = JsonPath.read(json, "$[*].name");
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, names);

        assertTrue(longs(json, "$[*].id").containsAll(List.of(C1, C2)));
        List<Boolean> disabledCategory = JsonPath.read(json, "$[?(@.id==" + C2 + ")].enabled");
        assertEquals(List.of(false), disabledCategory);
    }

    // ---------- estado de sincronizacion ----------

    @Test
    @DisplayName("Estado de sincronizacion: version y modo, con el error reducido a su tipo")
    void shouldReturnSyncStatusWithSanitizedError() throws Exception {
        syncVersionRepository.updateLastError(SyncVersion.CATALOG_KEY,
                "CatedraRedisException: No se pudo leer http://192.168.194.250:6379");
        entityManager.flush();
        entityManager.clear();

        String json = body(getWithToken("/api/catalog/sync/status")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber()));

        assertEquals("CatedraRedisException", JsonPath.read(json, "$.lastError"));
        assertFalse(json.contains("192.168"));
    }

    @Test
    @DisplayName("Sin error registrado, lastError es nulo")
    void shouldReturnNullLastErrorWhenThereIsNone() throws Exception {
        syncVersionRepository.updateLastError(SyncVersion.CATALOG_KEY, null);
        entityManager.flush();
        entityManager.clear();

        String json = body(getWithToken("/api/catalog/sync/status").andExpect(status().isOk()));

        assertNull(JsonPath.read(json, "$.lastError"));
    }
}