package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.CatalogDelta;

public interface ApplyCatalogDeltaUseCase {

    // Aplica 1 version en una unica transaccion. Es idempotente: una version ya aplicada no hace nada.
    void applyDelta(CatalogDelta delta);
}
