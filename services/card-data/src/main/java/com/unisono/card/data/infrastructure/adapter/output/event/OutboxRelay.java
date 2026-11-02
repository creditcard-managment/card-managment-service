package com.unisono.card.data.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Relay del outbox
 *
 * Publica en Kafka los eventos pendientes de outbox_events, en orden de inserción,
 * esperando la confirmación del broker. Ante un error:
 * - Transitorio (broker caído, timeout): registra el intento y corta el lote para no
 *   publicar fuera de orden; el evento se reintenta en la próxima pasada, sin límite.
 * - Permanente (payload inválido, mensaje demasiado grande, topic inválido): marca el
 *   evento como fallido (failed_at) y sigue con los siguientes, para que no bloquee la cola.
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
                if (isTransient(e)) {
                    event.markFailed(error);
                    log.error("❌ Outbox: failed to publish event {} ({}) to '{}', attempt {}, will retry: {}",
                            event.getEventId(), event.getEventType(), event.getTopic(), event.getAttempts(), error);
                    break;
                }
                event.markDead(error);
                log.error("☠️ Outbox: event {} ({}) to '{}' failed permanently and will not be retried: {}",
                        event.getEventId(), event.getEventType(), event.getTopic(), error);
            }
        }

        repository.saveAll(batch);
        return published;
    }

    /**
     * Un error es transitorio si en la cadena de causas hay una RetriableException de Kafka
     * (broker no disponible, timeout de metadata, líder no disponible...) o un timeout de espera.
     * Spring lo envuelve: ExecutionException → KafkaProducerException → causa real.
     */
    static boolean isTransient(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof RetriableException || t instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null
                ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                : cause.getClass().getSimpleName();
    }
}
