package com.unisono.card.data.infrastructure.adapter.output.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import com.unisono.card.events.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Adaptador del outbox
 *
 * Implementa EventOutboxPort guardando el evento serializado en outbox_events.
 * Exige una transacción activa: el evento se confirma junto con el cambio del aggregate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements EventOutboxPort {

    private final JpaOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String aggregateId, DomainEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .eventId(event.getEventId())
                .aggregateId(aggregateId)
                .topic(topic)
                .eventType(event.getEventType())
                .payload(serialize(event))
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(entity);

        log.debug("Event {} enqueued in outbox for topic '{}' (aggregate {})",
                event.getEventType(), topic, aggregateId);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + event.getEventType(), e);
        }
    }
}
