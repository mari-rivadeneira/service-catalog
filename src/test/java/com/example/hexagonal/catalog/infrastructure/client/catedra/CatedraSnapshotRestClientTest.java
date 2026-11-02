package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.application.exception.CatedraApiException;
import com.example.hexagonal.catalog.application.exception.InvalidCatedraSnapshotException;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatedraSnapshotRestClientTest {

    private static final String BASE_URL = "http://localhost:8090";
    private static final String TOKEN = "test-jwt-token";
    private static final String SNAPSHOT_URL = BASE_URL + "/api/synchronization/snapshot";

    private MockRestServiceServer mockServer;
    private CatedraSnapshotRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new CatedraSnapshotRestClient(restClient, new CatedraSnapshotMapper(), BASE_URL, TOKEN);
    }

    @Test
    void fetchSnapshot_Exito_RetornaSnapshotMapeado() {
        String jsonResponse = """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  "professionalCategories": [
                    {
                      "id": 1,
                      "name": "Clinica medica",
                      "description": "Atencion clinica general",
                      "enabled": true,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ],
                  "professionals": [
                    {
                      "id": 15,
                      "categoryId": 1,
                      "firstName": "Ana",
                      "lastName": "Gomez",
                      "enabled": true,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ],
                  "weeklySchedules": [
                    {
                      "id": 21,
                      "professionalId": 15,
                      "dayOfWeek": "MONDAY",
                      "startTime": "09:00:00",
                      "endTime": "12:00",
                      "slotDurationMinutes": 30,
                      "enabled": true,
                      "createdAt": "2026-07-01T12:00:00Z",
                      "updatedAt": "2026-09-10T13:40:00Z"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(SNAPSHOT_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CatedraSnapshot result = client.fetchSnapshot();

        mockServer.verify();

        assertThat(result.getVersion()).isEqualTo(7L);

        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().get(0).getId()).isEqualTo(1L);
        assertThat(result.getCategories().get(0).getName()).isEqualTo("Clinica medica");
        assertThat(result.getCategories().get(0).isEnabled()).isTrue();

        assertThat(result.getProfessionals()).hasSize(1);
        assertThat(result.getProfessionals().get(0).getId()).isEqualTo(15L);
        assertThat(result.getProfessionals().get(0).getCategoryId()).isEqualTo(1L);
        assertThat(result.getProfessionals().get(0).getLastName()).isEqualTo("Gomez");

        assertThat(result.getSchedules()).hasSize(1);
        assertThat(result.getSchedules().get(0).getId()).isEqualTo(21L);
        assertThat(result.getSchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.getSchedules().get(0).getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.getSchedules().get(0).getSlotDurationMinutes()).isEqualTo(30);
    }

    @Test
    void fetchSnapshot_ErrorProblemJson_ExponeStatusYCode() {
        String problem = """
                {
                  "title": "Forbidden",
                  "status": 403,
                  "detail": "Sin integracion tecnica",
                  "code": "STUDENT_INTEGRATION_NOT_FOUND"
                }
                """;

        mockServer.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.valueOf("application/problem+json"))
                        .body(problem));

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(403);
                    assertThat(e.getCode()).isEqualTo("STUDENT_INTEGRATION_NOT_FOUND");
                });

        mockServer.verify();
    }

    @Test
    void fetchSnapshot_ErrorServidorSinCuerpo_LanzaCatedraApiExceptionSinCode() {
        mockServer.expect(requestTo(SNAPSHOT_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(500);
                    assertThat(e.getCode()).isNull();
                });

        mockServer.verify();
    }

    @Test
    void fetchSnapshot_SinColecciones_LanzaInvalidCatedraSnapshotException() {
        mockServer.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withSuccess("{\"snapshotVersion\": 7}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOf(InvalidCatedraSnapshotException.class);

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