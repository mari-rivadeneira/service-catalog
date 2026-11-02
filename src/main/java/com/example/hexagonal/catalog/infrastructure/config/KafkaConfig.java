package com.example.hexagonal.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Reintentos acotados: 4 reintentos cada 2 segundos. Si siguen fallando se descarta el mensaje
     * (queda en el log); la reconciliacion por version recupera el atraso mas adelante.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(2_000L, 4L));
    }
}
