package com.unisono.card.payment.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaPaymentOutboxEventRepository;
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
 * Relay del outbox de pagos (payment_outbox_events)
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
public class PaymentOutboxRelay {

    private final JpaPaymentOutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.outbox.batch-size:50}")
    private int batchSize;

    /**
     * Debe superar delivery.timeout.ms + max.block.ms del productor (KafkaPaymentConfig)
     */
    @Value("${payment.outbox.send-timeout-ms:16000}")
    private long sendTimeoutMs;

    public PaymentOutboxRelay(
            JpaPaymentOutboxEventRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${payment.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        Integer published = transactionTemplate.execute(status -> publishBatch());
        if (published != null && published > 0) {
            log.info("📤 Payment outbox: {} event(s) published", published);
        }
    }

    /**
     * Publica el próximo lote dentro de la transacción actual.
     *
     * @return cantidad de eventos publicados
     */
    int publishBatch() {
        List<PaymentOutboxEventEntity> batch = repository.lockNextPendingBatch(batchSize);
        int published = 0;

        for (PaymentOutboxEventEntity event : batch) {
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
                    log.error("❌ Payment outbox: failed to publish event {} ({}) to '{}', attempt {}, will retry: {}",
                            event.getEventId(), event.getEventType(), event.getTopic(), event.getAttempts(), error);
                    break;
                }
                event.markDead(error);
                log.error("☠️ Payment outbox: event {} ({}) to '{}' failed permanently and will not be retried: {}",
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
