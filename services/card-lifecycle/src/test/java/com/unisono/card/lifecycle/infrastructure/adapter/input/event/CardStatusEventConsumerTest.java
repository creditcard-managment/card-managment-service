package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardActivatedEvent;
import com.unisono.card.events.domain.CardApprovedEvent;
import com.unisono.card.events.domain.CardBlockedEvent;
import com.unisono.card.events.domain.CardCancelledEvent;
import com.unisono.card.events.domain.CardExpiredEvent;
import com.unisono.card.events.domain.CardIssuedEvent;
import com.unisono.card.events.domain.CardRejectedEvent;
import com.unisono.card.events.domain.CardUnblockedEvent;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardStatusEventConsumerTest {

    private static final String CARD_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 13, 10, 15, 30);

    @Mock
    private JpaStateTransitionRepository repository;

    @Mock
    private Acknowledgment acknowledgment;

    private final ObjectMapper mapper = EventSerializationConfig.objectMapper();
    private CardStatusEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CardStatusEventConsumer(repository, mapper);
    }

    static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(KafkaTopics.CARD_APPROVED, new CardApprovedEvent(CARD_ID, "REQUESTED", "ok", "admin"), "REQUESTED", "APPROVED", "APPROVE", "admin"),
                Arguments.of(KafkaTopics.CARD_REJECTED, new CardRejectedEvent(CARD_ID, "REQUESTED", "no", "admin"), "REQUESTED", "REJECTED", "REJECT", "admin"),
                Arguments.of(KafkaTopics.CARD_ISSUED, new CardIssuedEvent(CARD_ID, "APPROVED", "0366", 12, 2030, "e", "system"), "APPROVED", "ISSUED", "ISSUE", "system"),
                Arguments.of(KafkaTopics.CARD_ACTIVATED, new CardActivatedEvent(CARD_ID, "ISSUED", AT, "a", "cliente"), "ISSUED", "ACTIVE", "ACTIVATE", "cliente"),
                Arguments.of(KafkaTopics.CARD_BLOCKED, new CardBlockedEvent(CARD_ID, "ACTIVE", "NORMAL", AT, "PIN", "system"), "ACTIVE", "BLOCKED", "BLOCK", "system"),
                Arguments.of(KafkaTopics.CARD_BLOCKED, new CardBlockedEvent(CARD_ID, "ACTIVE", "FRAUD", AT, "f", "antifraude"), "ACTIVE", "BLOCKED_FRAUD", "BLOCK_FRAUD", "antifraude"),
                Arguments.of(KafkaTopics.CARD_UNBLOCKED, new CardUnblockedEvent(CARD_ID, "BLOCKED_FRAUD", AT, "u", "admin"), "BLOCKED_FRAUD", "ACTIVE", "UNBLOCK", "admin"),
                Arguments.of(KafkaTopics.CARD_CANCELLED, new CardCancelledEvent(CARD_ID, "BLOCKED", AT, "c", "cliente"), "BLOCKED", "CANCELLED", "CANCEL", "cliente"),
                Arguments.of(KafkaTopics.CARD_EXPIRED, new CardExpiredEvent(CARD_ID, "ACTIVE", AT, "Vencimiento por fecha (11/26)", "system"), "ACTIVE", "EXPIRED", "EXPIRE", "system")
        );
    }

    @ParameterizedTest
    @MethodSource("events")
    void registersEachChangeAsASuccessfulTransition(String topic, DomainEvent event, String from, String to,
                                                    String lifecycleEvent, String actor) throws Exception {
        when(repository.existsByEventId(event.getEventId())).thenReturn(false);

        consumer.consume(mapper.writeValueAsString(event), topic, acknowledgment);

        StateTransitionEntity saved = saved();
        assertThat(saved.getCardId()).isEqualTo(CARD_ID);
        assertThat(saved.getFromState()).isEqualTo(from);
        assertThat(saved.getToState()).isEqualTo(to);
        assertThat(saved.getEvent()).isEqualTo(lifecycleEvent);
        assertThat(saved.getPerformedBy()).isEqualTo(actor);
        assertThat(saved.getSuccessful()).isTrue();
        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getMetadata()).isEqualTo("{\"eventId\":\"" + event.getEventId() + "\"}");
        assertThat(saved.getTransitionedAt()).isNotNull();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void usesTheDateOfTheChange() throws Exception {
        CardBlockedEvent event = new CardBlockedEvent(CARD_ID, "ACTIVE", "NORMAL", AT, "b", "admin");

        consumer.consume(mapper.writeValueAsString(event), KafkaTopics.CARD_BLOCKED, acknowledgment);

        assertThat(saved().getTransitionedAt()).isEqualTo(AT);
    }

    @Test
    void duplicateEventIsAcknowledgedWithoutRegisteringItAgain() throws Exception {
        CardCancelledEvent event = new CardCancelledEvent(CARD_ID, "ACTIVE", AT, "c", "cliente");
        when(repository.existsByEventId(event.getEventId())).thenReturn(true);

        consumer.consume(mapper.writeValueAsString(event), KafkaTopics.CARD_CANCELLED, acknowledgment);

        verify(repository, never()).saveAndFlush(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void concurrentDuplicateIsSkipped() throws Exception {
        CardExpiredEvent event = new CardExpiredEvent(CARD_ID, "ACTIVE", AT, "v", "system");
        when(repository.existsByEventId(event.getEventId())).thenReturn(false, true);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_state_transitions_event_id"));

        consumer.consume(mapper.writeValueAsString(event), KafkaTopics.CARD_EXPIRED, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void otherConstraintViolationIsNotSwallowed() throws Exception {
        CardExpiredEvent event = new CardExpiredEvent(CARD_ID, "ACTIVE", AT, "v", "system");
        when(repository.existsByEventId(event.getEventId())).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("fk_state_transitions_card"));

        assertThatThrownBy(() -> consumer.consume(mapper.writeValueAsString(event), KafkaTopics.CARD_EXPIRED, acknowledgment))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void messageWithoutPreviousStatusOrActorIsStillRegistered() throws Exception {
        String old = """
                {"eventId":"e-1","occurredOn":"2026-11-13T10:15:30","eventType":"CardBlockedEvent",
                 "cardId":"%s","blockType":"NORMAL","blockedAt":"2026-11-13T10:15:30","reason":"PIN"}
                """.formatted(CARD_ID);

        consumer.consume(old, KafkaTopics.CARD_BLOCKED, acknowledgment);

        StateTransitionEntity saved = saved();
        assertThat(saved.getFromState()).isNull();
        assertThat(saved.getToState()).isEqualTo("BLOCKED");
        assertThat(saved.getPerformedBy()).isEqualTo("system");
    }

    private StateTransitionEntity saved() {
        ArgumentCaptor<StateTransitionEntity> captor = ArgumentCaptor.forClass(StateTransitionEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
