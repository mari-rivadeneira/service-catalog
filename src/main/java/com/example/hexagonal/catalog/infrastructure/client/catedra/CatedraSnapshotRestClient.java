package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.application.exception.CatedraApiException;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.port.out.CatedraSnapshotClient;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Component
public class CatedraSnapshotRestClient implements CatedraSnapshotClient {

    private final RestClient restClient;
    private final CatedraSnapshotMapper mapper;
    private final String baseUrl;
    private final String token;

    @Autowired
    public CatedraSnapshotRestClient(
            @Value("${catedra.api.base-url:}") String baseUrl,
            @Value("${catedra.api.jwt-token:}") String token,
            @Value("${catedra.api.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${catedra.api.read-timeout-ms:10000}") int readTimeoutMs,
            CatedraSnapshotMapper mapper) {
        this(buildRestClient(connectTimeoutMs, readTimeoutMs), mapper, baseUrl, token);
    }

    public CatedraSnapshotRestClient(RestClient restClient, CatedraSnapshotMapper mapper,
                                     String baseUrl, String token) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.token = token;
    }

    private static RestClient buildRestClient(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public CatedraSnapshot fetchSnapshot() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("La URL de la API de cátedra no está configurada");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("El JWT técnico de cátedra no está configurado");
        }

        CatedraSnapshotResponseDto response = restClient.get()
                .uri(baseUrl + "/api/synchronization/snapshot")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange((request, httpResponse) -> {
                    if (httpResponse.getStatusCode().isError()) {
                        throw toException(httpResponse);
                    }
                    return httpResponse.bodyTo(CatedraSnapshotResponseDto.class);
                });

        if (response == null) {
            throw new IllegalStateException("La API de cátedra devolvió un snapshot vacío");
        }

        return mapper.toDomain(response);
    }

    private CatedraApiException toException(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
            throws IOException {
        int status = response.getStatusCode().value();
        String code = null;
        String detail = null;
        try {
            ProblemDto problem = response.bodyTo(ProblemDto.class);
            if (problem != null) {
                code = problem.code();
                detail = problem.detail();
            }
        } catch (Exception ignored) {

        }
        return new CatedraApiException(status, code, detail);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProblemDto(String code, String detail) {
    }
}