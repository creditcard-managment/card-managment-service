package com.unisono.card.data.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relay del outbox
 *
 * Publica en Kafka los eventos pendientes de outbox_events, en orden de inserción,
 * esperando la confirmación del broker. Si un envío falla, registra el error y corta
 * el lote para no publicar fuera de orden; el evento se reintenta en la próxima pasada.
 *
 * Semántica at-least-once: si la instancia cae entre el envío y el commit,
 * el evento se vuelve a publicar. Los consumidores deben deduplicar por eventId.
 */
@Slf4j
@Component
public class OutboxRelay {

    private final JpaOutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${card.data.outbox.batch-size:50}")
    private int batchSize;

    /**
     * Debe superar delivery.timeout.ms + max.block.ms del productor (KafkaProducerConfig)
     */
    @Value("${card.data.outbox.send-timeout-ms:15000}")
    private long sendTimeoutMs;

    public OutboxRelay(
            JpaOutboxEventRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${card.data.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        Integer published = transactionTemplate.execute(status -> publishBatch());
        if (published != null && published > 0) {
            log.info("📤 Outbox: {} event(s) published", published);
        }
    }

    /**
     * Publica el próximo lote dentro de la transacción actual.
     *
     * @return cantidad de eventos publicados
     */
    int publishBatch() {
        List<OutboxEventEntity> batch = repository.lockNextPendingBatch(batchSize);
        int published = 0;

        for (OutboxEventEntity event : batch) {
            try {
                // Se envía el JSON almacenado tal cual (JsonNode) para no alterar el payload
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), objectMapper.readTree(event.getPayload()))
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                event.markPublished();
                published++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.markFailed("Interrupted");
                break;
            } catch (Exception e) {
                String error = describe(e);
                event.markFailed(error);
                log.error("❌ Outbox: failed to publish event {} ({}) to '{}', attempt {}: {}",
                        event.getEventId(), event.getEventType(), event.getTopic(), event.getAttempts(), error);
                break;
            }
        }

        repository.saveAll(batch);
        return published;
    }

    private static String describe(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage() != null
                ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                : cause.getClass().getSimpleName();
    }
}
