package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.config.KafkaConsumerConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reintentos, DLQ y commit de offsets del consumidor contra un broker Kafka real (embebido).
 */
@SpringJUnitConfig(CardEventConsumerDeadLetterTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.CARD_CREATED, KafkaTopics.DLQ_CARD_EVENTS})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=" + CardEventConsumerDeadLetterTest.GROUP,
        "card-lifecycle.kafka.retry.max-retries=2",
        "card-lifecycle.kafka.retry.initial-interval-ms=50"
})
@DirtiesContext
class CardEventConsumerDeadLetterTest {

    static final String GROUP = "dead-letter-test";
    private static final int ATTEMPTS = 3; // el original + max-retries
    private static final long WAIT_MS = 20_000;

    @Configuration
    @EnableKafka
    @Import({KafkaConsumerConfig.class, CardEventConsumer.class})
    static class TestConfig {
    }

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private JpaStateTransitionRepository repository;

    @SpyBean(name = "kafkaObjectMapper")
    private ObjectMapper kafkaObjectMapper;

    @Test
    void unreadableMessageGoesToDlqWithoutRetries() throws Exception {
        String key = UUID.randomUUID().toString();

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, key, "{not json").get();

        ConsumerRecord<String, String> dead = awaitInDlq(key);
        assertThat(dead.value()).isEqualTo("{not json");
        assertThat(header(dead, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(KafkaTopics.CARD_CREATED);
        assertThat(header(dead, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains("Unexpected character");
        verify(kafkaObjectMapper, times(1)).readValue("{not json", CardCreatedEvent.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void transientFailureIsRetriedAndNotSentToDlq() throws Exception {
        String cardId = UUID.randomUUID().toString();
        when(repository.existsByCardIdAndEvent(cardId, "CREATE"))
                .thenThrow(new DataAccessResourceFailureException("db down"))
                .thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, cardId, eventJson(cardId)).get();

        verify(repository, timeout(WAIT_MS)).saveAndFlush(argThat(t -> cardId.equals(t.getCardId())));
        awaitEverythingCommitted();
        assertThat(findInDlq(cardId)).isEmpty();
    }

    @Test
    void persistentFailureGoesToDlqAndDoesNotBlockNextMessages() throws Exception {
        String poisonId = UUID.randomUUID().toString();
        String nextId = UUID.randomUUID().toString();
        when(repository.existsByCardIdAndEvent(poisonId, "CREATE"))
                .thenThrow(new DataAccessResourceFailureException("no se puede procesar"));
        when(repository.existsByCardIdAndEvent(nextId, "CREATE")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, poisonId, eventJson(poisonId)).get();
        kafkaTemplate.send(KafkaTopics.CARD_CREATED, nextId, eventJson(nextId)).get();

        ConsumerRecord<String, String> dead = awaitInDlq(poisonId);
        assertThat(dead.value()).contains(poisonId);
        verify(repository, timeout(WAIT_MS)).saveAndFlush(argThat(t -> nextId.equals(t.getCardId())));
        verify(repository, times(ATTEMPTS)).existsByCardIdAndEvent(eq(poisonId), eq("CREATE"));
        awaitEverythingCommitted();
    }

    @Test
    void offsetOfDeadLetteredMessageIsCommittedEvenIfItIsTheLastOne() throws Exception {
        String poisonId = UUID.randomUUID().toString();
        when(repository.existsByCardIdAndEvent(poisonId, "CREATE"))
                .thenThrow(new DataAccessResourceFailureException("no se puede procesar"));

        kafkaTemplate.send(KafkaTopics.CARD_CREATED, poisonId, eventJson(poisonId)).get();

        awaitInDlq(poisonId);
        awaitEverythingCommitted();
    }

    // ========== Helpers ==========

    private static String eventJson(String cardId) throws Exception {
        return EventSerializationConfig.objectMapper().writeValueAsString(
                new CardCreatedEvent(cardId, "12345678", "Juan Pérez", "VISA", "CLASSIC", "system"));
    }

    private ConsumerRecord<String, String> awaitInDlq(String key) {
        List<ConsumerRecord<String, String>> found = new ArrayList<>();
        await().atMost(Duration.ofMillis(WAIT_MS)).until(() -> {
            found.clear();
            findInDlq(key).ifPresent(found::add);
            return !found.isEmpty();
        });
        return found.get(0);
    }

    private Optional<ConsumerRecord<String, String>> findInDlq(String key) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.DLQ_CARD_EVENTS);
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1))) {
                if (key.equals(record.key())) {
                    return Optional.of(record);
                }
            }
            return Optional.empty();
        }
    }

    /** Un reinicio del consumidor no volvería a procesar ningún mensaje de card.created. */
    private void awaitEverythingCommitted() {
        TopicPartition partition = new TopicPartition(KafkaTopics.CARD_CREATED, 0);
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()))) {
            await().atMost(Duration.ofMillis(WAIT_MS)).untilAsserted(() -> {
                long end = admin.listOffsets(Map.of(partition, OffsetSpec.latest())).partitionResult(partition).get().offset();
                OffsetAndMetadata committed = admin.listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata().get().get(partition);
                assertThat(committed).isNotNull();
                assertThat(committed.offset()).isEqualTo(end);
            });
        }
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
