package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatedraSnapshotRestClientTest {

    private static final String BASE_URL = "http://localhost:8090";
    private static final String TOKEN = "test-jwt-token";

    private MockRestServiceServer mockServer;
    private CatedraSnapshotRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        CatedraSnapshotMapper mapper = new CatedraSnapshotMapper();
        client = new CatedraSnapshotRestClient(restClient, mapper, BASE_URL, TOKEN);
    }

    @Test
    void fetchSnapshot_Exito_RetornaSnapshotMapeado() {
        UUID categoryId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        String jsonResponse = """
                {
                  "version": 1,
                  "categories": [
                    {
                      "id": "%s",
                      "name": "Medicina General",
                      "description": "Consultas clínicas",
                      "createdAt": "2026-01-01T00:00:00Z",
                      "updatedAt": "2026-01-02T00:00:00Z"
                    }
                  ],
                  "professionals": [
                    {
                      "id": "%s",
                      "firstName": "Lucia",
                      "lastName": "Perez",
                      "email": "lucia@test.com",
                      "phone": "2611234567",
                      "categoryId": "%s",
                      "active": true,
                      "createdAt": "2026-01-01T00:00:00Z",
                      "updatedAt": "2026-01-02T00:00:00Z"
                    }
                  ],
                  "schedules": [
                    {
                      "id": "%s",
                      "professionalId": "%s",
                      "dayOfWeek": "MONDAY",
                      "startTime": "08:00:00",
                      "endTime": "12:00:00",
                      "slotDurationMinutes": 30
                    }
                  ]
                }
                """.formatted(categoryId, professionalId, categoryId, scheduleId, professionalId);

        mockServer.expect(requestTo(BASE_URL + "/api/synchronization/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CatedraSnapshot result = client.fetchSnapshot();

        mockServer.verify();

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1L);

        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().get(0).getId()).isEqualTo(categoryId);
        assertThat(result.getCategories().get(0).getName()).isEqualTo("Medicina General");

        assertThat(result.getProfessionals()).hasSize(1);
        assertThat(result.getProfessionals().get(0).getId()).isEqualTo(professionalId);
        assertThat(result.getProfessionals().get(0).getLastName()).isEqualTo("Perez");

        assertThat(result.getSchedules()).hasSize(1);
        assertThat(result.getSchedules().get(0).getId()).isEqualTo(scheduleId);
        assertThat(result.getSchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void fetchSnapshot_ErrorServidor_LanzaExcepcion() {
        mockServer.expect(requestTo(BASE_URL + "/api/synchronization/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOf(RuntimeException.class);

        mockServer.verify();
    }

    @Test
    void fetchSnapshot_SinConfiguracion_LanzaIllegalStateException() {
        CatedraSnapshotRestClient clientSinUrl = new CatedraSnapshotRestClient(
                RestClient.builder().build(),
                new CatedraSnapshotMapper(),
                "",
                TOKEN
        );

        assertThatThrownBy(clientSinUrl::fetchSnapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La URL de la API de cátedra no está configurada");
    }
}