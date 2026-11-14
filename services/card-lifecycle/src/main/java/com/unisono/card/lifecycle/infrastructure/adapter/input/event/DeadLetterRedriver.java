package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unisono.card.events.config.KafkaTopics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reprocesa automáticamente los mensajes de dlq.card-events: los vuelve a publicar en su topic de origen,
 * con la misma key y el mismo valor, después de una espera que crece en cada intento.
 * <ul>
 *   <li>La espera se cuenta desde que el mensaje entró a la DLQ (timestamp del registro):
 *       {@code initial-delay × multiplier^intentos}. Mientras no se cumple, la partición de la DLQ queda pausada
 *       ({@link Acknowledgment#nack(Duration)}) y el mensaje se vuelve a leer al terminar la espera.</li>
 *   <li>El número de intento viaja en el header {@value #REDRIVE_COUNT_HEADER}. Si el mensaje vuelve a fallar,
 *       {@code DeadLetterPublishingRecoverer} copia los headers y lo devuelve a la DLQ con el contador.</li>
 *   <li>No se reprocesan, y quedan en la DLQ para revisión manual: los mensajes ilegibles (JSON inválido,
 *       que nunca van a funcionar), los que agotaron los intentos y los de un topic que este servicio no consume.</li>
 * </ul>
 * Los consumidores (card.created y el historial de cambios de estado) son idempotentes, así que reinyectar un
 * mensaje ya procesado no tiene efecto.
 * Si la publicación falla, el mensaje no se confirma y se reintenta (ver {@code dlqRedriveContainerFactory}).
 */
@Slf4j
@Component
public class DeadLetterRedriver {

    public static final String REDRIVE_COUNT_HEADER = "x-dlq-redrive-count";

    /** Topics cuyo consumidor está en este servicio y manda a dlq.card-events. */
    private static final Set<String> REDRIVABLE_TOPICS = redrivableTopics();
    private static final String DLT_HEADER_PREFIX = "kafka_dlt-";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int maxAttempts;
    private final long initialDelayMs;
    private final double multiplier;
    private final long sendTimeoutMs;
    private final Counter redriven;
    private final Counter parkedUnreadable;
    private final Counter parkedExhausted;
    private final Counter parkedUnknownTopic;

    public DeadLetterRedriver(
            @Qualifier("deadLetterKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${card-lifecycle.kafka.dlq-redrive.max-attempts:5}") int maxAttempts,
            @Value("${card-lifecycle.kafka.dlq-redrive.initial-delay-ms:60000}") long initialDelayMs,
            @Value("${card-lifecycle.kafka.dlq-redrive.multiplier:2}") double multiplier,
            @Value("${card-lifecycle.kafka.dlq-redrive.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
        this.sendTimeoutMs = sendTimeoutMs;
        this.redriven = counter(meterRegistry, "redriven");
        this.parkedUnreadable = counter(meterRegistry, "parked_unreadable");
        this.parkedExhausted = counter(meterRegistry, "parked_exhausted");
        this.parkedUnknownTopic = counter(meterRegistry, "parked_unknown_topic");
    }

    @KafkaListener(
            id = "dlq-redrive",
            topics = KafkaTopics.DLQ_CARD_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}-dlq-redrive",
            containerFactory = "dlqRedriveContainerFactory",
            autoStartup = "${card-lifecycle.kafka.dlq-redrive.enabled:true}"
    )
    public void redrive(ConsumerRecord<String, String> record, Acknowledgment acknowledgment)
            throws InterruptedException, ExecutionException, TimeoutException {
        String originalTopic = header(record.headers(), KafkaHeaders.DLT_ORIGINAL_TOPIC);
        int attempts = redriveCount(record.headers());

        if (originalTopic == null || !REDRIVABLE_TOPICS.contains(originalTopic)) {
            park(record, acknowledgment, parkedUnknownTopic, "unknown original topic " + originalTopic);
            return;
        }
        if (isUnreadable(record.headers())) {
            park(record, acknowledgment, parkedUnreadable, "unreadable message, it will never succeed");
            return;
        }
        if (attempts >= maxAttempts) {
            park(record, acknowledgment, parkedExhausted, "failed after " + attempts + " redrives");
            return;
        }

        long waitMs = record.timestamp() + delayMs(attempts) - System.currentTimeMillis();
        if (waitMs > 0) {
            // Pausa la partición sin bloquear el poll; el mensaje se vuelve a leer al terminar la espera
            acknowledgment.nack(Duration.ofMillis(waitMs));
            return;
        }

        ProducerRecord<String, String> retry = new ProducerRecord<>(
                originalTopic, null, record.key(), record.value(), redriveHeaders(record.headers(), attempts + 1));
        kafkaTemplate.send(retry).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
        acknowledgment.acknowledge();
        redriven.increment();
        log.info("Redriven DLQ message {}-{}@{} (key {}) to {}, attempt {}/{}", record.topic(), record.partition(),
                record.offset(), record.key(), originalTopic, attempts + 1, maxAttempts);
    }

    private static Set<String> redrivableTopics() {
        Set<String> topics = new java.util.HashSet<>(CardStatusEventConsumer.TOPICS);
        topics.add(KafkaTopics.CARD_CREATED);
        return Set.copyOf(topics);
    }

    long delayMs(int attempts) {
        return (long) (initialDelayMs * Math.pow(multiplier, attempts));
    }

    private void park(ConsumerRecord<String, String> record, Acknowledgment acknowledgment, Counter counter,
                      String reason) {
        acknowledgment.acknowledge();
        counter.increment();
        log.error("DLQ message {}-{}@{} (key {}) left for manual review: {}. Exception: {}", record.topic(),
                record.partition(), record.offset(), record.key(), reason,
                header(record.headers(), KafkaHeaders.DLT_EXCEPTION_MESSAGE));
    }

    /** El error fue de lectura del JSON (no se reintentó al consumirlo, ver KafkaConsumerConfig). */
    private static boolean isUnreadable(Headers headers) {
        return isJsonError(header(headers, KafkaHeaders.DLT_EXCEPTION_FQCN))
                || isJsonError(header(headers, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN));
    }

    private static boolean isJsonError(String className) {
        if (className == null) {
            return false;
        }
        try {
            return JsonProcessingException.class.isAssignableFrom(Class.forName(className));
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Headers originales del mensaje, sin los de la DLQ (los vuelve a poner el recoverer si falla otra vez). */
    private static Headers redriveHeaders(Headers headers, int attempt) {
        RecordHeaders result = new RecordHeaders();
        for (Header h : headers) {
            if (!h.key().startsWith(DLT_HEADER_PREFIX) && !h.key().equals(REDRIVE_COUNT_HEADER)) {
                result.add(h);
            }
        }
        result.add(REDRIVE_COUNT_HEADER, Integer.toString(attempt).getBytes(StandardCharsets.UTF_8));
        return result;
    }

    private static int redriveCount(Headers headers) {
        String value = header(headers, REDRIVE_COUNT_HEADER);
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String header(Headers headers, String name) {
        Header h = headers.lastHeader(name);
        return h == null || h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static Counter counter(MeterRegistry registry, String outcome) {
        return Counter.builder("card.lifecycle.dlq.redrive")
                .description("Messages read from dlq.card-events by outcome")
                .tag("outcome", outcome)
                .register(registry);
    }
}
