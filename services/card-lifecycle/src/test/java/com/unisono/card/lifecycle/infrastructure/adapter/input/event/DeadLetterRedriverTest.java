package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.config.KafkaConsumerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproceso automático de dlq.card-events contra un broker Kafka real (embebido): consumidor de card.created,
 * su DLQ y el reprocesador funcionando juntos.
 */
@SpringJUnitConfig(DeadLetterRedriverTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.CARD_CREATED, KafkaTopics.DLQ_CARD_EVENTS})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=dlq-redrive-test",
        "card-lifecycle.kafka.retry.max-retries=1",
        "card-lifecycle.kafka.retry.initial-interval-ms=50",
        "card-lifecycle.kafka.dlq-redrive.max-attempts=2",
        "card-lifecycle.kafka.dlq-redrive.initial-delay-ms=" + DeadLetterRedriverTest.DELAY_MS,
        "card-lifecycle.kafka.dlq-redrive.multiplier=1",
        "card-lifecycle.kafka.dlq-redrive.retry-interval-ms=100"
})
@DirtiesContext
class DeadLetterRedriverTest {

    static final long DELAY_MS = 1_000;
    private static final long WAIT_MS = 30_000;

    @Configuration
    @EnableKafka
    @Import({KafkaConsumerConfig.class, CardEventConsumer.class, DeadLetterRedriver.class})
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private JpaStateTransitionRepository repository;

    @Test
    void messageThatFailedDuringAnOutageIsRedrivenAfterTheDelayAndProcessed() throws Exception {
        String cardId = UUID.randomUUID().toString();
        double redrivenBefore = count("redriven");
        when(repository.existsByCardIdAndEvent(cardId, "CREATE"))
                // La caída dura más que los dos intentos del consumidor: el mensaje va a la DLQ
                .thenThrow(new DataAccessResourceFailureException("db down"))
                .thenThrow(new DataAccessResourceFailureException("db down"))
                .thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, cardId, eventJson(cardId)).get();

        verify(repository, timeout(WAIT_MS)).saveAndFlush(argThat(t -> cardId.equals(t.getCardId())));
        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> count("redriven") == redrivenBefore + 1);

        List<ConsumerRecord<String, String>> dead = records(KafkaTopics.DLQ_CARD_EVENTS, cardId);
        List<ConsumerRecord<String, String>> sent = records(KafkaTopics.CARD_CREATED, cardId);
        assertThat(dead).hasSize(1);
        assertThat(sent).hasSize(2);
        ConsumerRecord<String, String> redriven = sent.get(1);
        assertThat(redriven.value()).isEqualTo(sent.get(0).value());
        assertThat(header(redriven, DeadLetterRedriver.REDRIVE_COUNT_HEADER)).isEqualTo("1");
        assertThat(redriven.headers().toArray()).noneMatch(h -> h.key().startsWith("kafka_dlt-"));
        // Esperó el delay desde que el mensaje entró a la DLQ
        assertThat(redriven.timestamp() - dead.get(0).timestamp()).isGreaterThanOrEqualTo(DELAY_MS);
    }

    @Test
    void persistentFailureIsRedrivenUpToMaxAttemptsAndThenLeftInTheDlq() throws Exception {
        String cardId = UUID.randomUUID().toString();
        double exhaustedBefore = count("parked_exhausted");
        when(repository.existsByCardIdAndEvent(cardId, "CREATE"))
                .thenThrow(new DataAccessResourceFailureException("no se puede procesar"));

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, cardId, eventJson(cardId)).get();

        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> count("parked_exhausted") > exhaustedBefore);

        // El original y dos reprocesos fallaron: tres pasadas por la DLQ, con el contador de intentos
        List<ConsumerRecord<String, String>> dead = records(KafkaTopics.DLQ_CARD_EVENTS, cardId);
        assertThat(dead).extracting(r -> header(r, DeadLetterRedriver.REDRIVE_COUNT_HEADER))
                .containsExactly(null, "1", "2");
        assertThat(records(KafkaTopics.CARD_CREATED, cardId)).hasSize(3);
        verify(repository, never()).saveAndFlush(argThat(t -> cardId.equals(t.getCardId())));
    }

    @Test
    void unreadableMessageIsNotRedriven() throws Exception {
        String key = UUID.randomUUID().toString();
        double unreadableBefore = count("parked_unreadable");

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, key, "{not json").get();

        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> count("parked_unreadable") > unreadableBefore);
        assertThat(records(KafkaTopics.DLQ_CARD_EVENTS, key)).hasSize(1);
        assertThat(records(KafkaTopics.CARD_CREATED, key)).hasSize(1);
    }

    @Test
    void delayGrowsWithEachAttempt() {
        DeadLetterRedriver redriver = new DeadLetterRedriver(kafkaTemplate, new SimpleMeterRegistry(), 5, 60_000, 2, 1_000);

        assertThat(List.of(0, 1, 2, 3, 4)).extracting(redriver::delayMs)
                .containsExactly(60_000L, 120_000L, 240_000L, 480_000L, 960_000L);
    }

    // ========== Helpers ==========

    private double count(String outcome) {
        return meterRegistry.get("card.lifecycle.dlq.redrive").tag("outcome", outcome).counter().count();
    }

    private static String eventJson(String cardId) throws Exception {
        return EventSerializationConfig.objectMapper().writeValueAsString(
                new CardCreatedEvent(cardId, "12345678", "Juan Pérez", "VISA", "CLASSIC", "system"));
    }

    private List<ConsumerRecord<String, String>> records(String topic, String key) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> result = new ArrayList<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, topic);
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1))) {
                if (key.equals(record.key())) {
                    result.add(record);
                }
            }
        }
        return result;
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}
