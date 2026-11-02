package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.port.in.ProcessCatalogUpdatedEventUseCase;
import com.example.hexagonal.catalog.domain.port.in.SyncCatalogIncrementalUseCase;
import com.example.hexagonal.catalog.domain.port.out.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * NO es transaccional a proposito: el evento se registra recien cuando la sincronizacion termino bien.
 * Si el proceso cae antes de registrarlo, el evento se reprocesa y la comparacion de versiones lo vuelve inofensivo.
 */
@Service
public class ProcessCatalogUpdatedEventUseCaseImpl implements ProcessCatalogUpdatedEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessCatalogUpdatedEventUseCaseImpl.class);

    private final ProcessedEventRepository processedEventRepository;
    private final SyncCatalogIncrementalUseCase syncCatalogIncrementalUseCase;

    public ProcessCatalogUpdatedEventUseCaseImpl(
            ProcessedEventRepository processedEventRepository,
            SyncCatalogIncrementalUseCase syncCatalogIncrementalUseCase) {
        this.processedEventRepository = processedEventRepository;
        this.syncCatalogIncrementalUseCase = syncCatalogIncrementalUseCase;
    }

    @Override
    public void process(String eventId, long newVersion) {
        if (processedEventRepository.exists(eventId)) {
            log.info("Evento {} ya procesado: se ignora", eventId);
            return;
        }
        syncCatalogIncrementalUseCase.sync(newVersion);
        processedEventRepository.markProcessed(eventId);
    }
}
