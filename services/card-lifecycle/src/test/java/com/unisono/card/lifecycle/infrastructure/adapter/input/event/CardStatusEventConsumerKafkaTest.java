package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardBlockedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import com.unisono.card.lifecycle.infrastructure.config.KafkaConsumerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Historial contra un broker Kafka real (embebido): el listener de los topics de estado, con sus reintentos,
 * la DLQ y el reproceso automático de DeadLetterRedriver.
 */
@SpringJUnitConfig(CardStatusEventConsumerKafkaTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopics.CARD_APPROVED, KafkaTopics.CARD_REJECTED, KafkaTopics.CARD_ISSUED, KafkaTopics.CARD_ACTIVATED,
        KafkaTopics.CARD_BLOCKED, KafkaTopics.CARD_UNBLOCKED, KafkaTopics.CARD_CANCELLED, KafkaTopics.CARD_EXPIRED,
        KafkaTopics.CARD_CREATED, KafkaTopics.DLQ_CARD_EVENTS})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=history-test",
        "card-lifecycle.kafka.history.auto-offset-reset=earliest",
        "card-lifecycle.kafka.retry.max-retries=1",
        "card-lifecycle.kafka.retry.initial-interval-ms=50",
        "card-lifecycle.kafka.dlq-redrive.initial-delay-ms=500",
        "card-lifecycle.kafka.dlq-redrive.multiplier=1"
})
@DirtiesContext
class CardStatusEventConsumerKafkaTest {

    private static final long WAIT_MS = 30_000;

    @Configuration
    @EnableKafka
    @Import({KafkaConsumerConfig.class, CardEventConsumer.class, CardStatusEventConsumer.class, DeadLetterRedriver.class})
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private JpaStateTransitionRepository repository;

    @Test
    void blockedEventPublishedByCardDataEndsUpInTheHistory() throws Exception {
        String cardId = UUID.randomUUID().toString();
        CardBlockedEvent event = new CardBlockedEvent(cardId, "ACTIVE", "NORMAL", LocalDateTime.now(),
                "Bloqueo automático: 3 intentos fallidos de PIN", "system");
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        kafkaTemplate.send(KafkaTopics.CARD_BLOCKED, cardId, json(event)).get();

        verify(repository, timeout(WAIT_MS)).saveAndFlush(argThat(t -> cardId.equals(t.getCardId())
                && "BLOCK".equals(t.getEvent()) && "ACTIVE".equals(t.getFromState())
                && event.getEventId().equals(t.getEventId())));
    }

    @Test
    void changeThatFailedDuringAnOutageIsRedrivenFromTheDlqAndRegistered() throws Exception {
        String cardId = UUID.randomUUID().toString();
        CardBlockedEvent event = new CardBlockedEvent(cardId, "ACTIVE", "FRAUD", LocalDateTime.now(), "f", "antifraude");
        // Falla en los dos intentos del consumidor (va a la DLQ) y funciona en el reproceso
        when(repository.existsByEventId(event.getEventId()))
                .thenThrow(new DataAccessResourceFailureException("db down"))
                .thenThrow(new DataAccessResourceFailureException("db down"))
                .thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        kafkaTemplate.send(KafkaTopics.CARD_BLOCKED, cardId, json(event)).get();

        verify(repository, timeout(WAIT_MS)).saveAndFlush(argThat(t -> cardId.equals(t.getCardId())
                && "BLOCK_FRAUD".equals(t.getEvent())));
    }

    private static String json(Object event) throws Exception {
        return EventSerializationConfig.objectMapper().writeValueAsString(event);
    }
}
