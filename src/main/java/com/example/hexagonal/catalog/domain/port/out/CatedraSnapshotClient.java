package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;

public interface CatedraSnapshotClient {

    /**
     * Obtiene el snapshot completo del catálogo provisto por la cátedra.
     *
     * @return Contenedor con versión, categorías, profesionales y disponibilidades semanales.
     */
    CatedraSnapshot fetchSnapshot();
}