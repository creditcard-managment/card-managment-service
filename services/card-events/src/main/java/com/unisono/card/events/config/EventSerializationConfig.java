package com.unisono.card.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Serialización JSON de los eventos, común a productores y consumidores:
 * soporte de java.time y fechas en formato ISO, no como timestamps numéricos.
 */
public final class EventSerializationConfig {

    private EventSerializationConfig() {
        throw new AssertionError("No instances allowed");
    }

    public static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
