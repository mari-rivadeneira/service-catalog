package com.example.hexagonal.catalog.infrastructure.client.catedra;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.port.out.CatedraSnapshotClient;
import com.example.hexagonal.catalog.infrastructure.client.catedra.dto.CatedraSnapshotResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
            CatedraSnapshotMapper mapper) {
        this.restClient = RestClient.builder().build();
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public CatedraSnapshotRestClient(RestClient restClient, CatedraSnapshotMapper mapper, String baseUrl, String token) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.token = token;
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
                .retrieve()
                .body(CatedraSnapshotResponseDto.class);

        if (response == null) {
            throw new IllegalStateException("La API de cátedra devolvió un snapshot vacío");
        }

        return mapper.toDomain(response);
    }
}