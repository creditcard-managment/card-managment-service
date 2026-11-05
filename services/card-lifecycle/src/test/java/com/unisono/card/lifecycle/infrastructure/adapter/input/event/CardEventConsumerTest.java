package com.unisono.card.lifecycle.infrastructure.adapter.input.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity.StateTransitionEntity;
import com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.repository.JpaStateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardEventConsumerTest {

    private static final String CARD_ID = "be546553-f910-4891-8881-e83e1e7f488f";

    @Mock
    private JpaStateTransitionRepository repository;

    @Mock
    private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private CardEventConsumer consumer;
    private String eventJson;

    @BeforeEach
    void setUp() throws Exception {
        consumer = new CardEventConsumer(repository, objectMapper);
        eventJson = objectMapper.writeValueAsString(
                new CardCreatedEvent(CARD_ID, "12345678", "Juan Pérez", "VISA", "CLASSIC", "system"));
    }

    @Test
    void shouldRegisterCreateTransitionForNewCard() throws Exception {
        when(repository.existsByCardIdAndEvent(CARD_ID, "CREATE")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeCardCreatedEvent(eventJson, 0, 0L, acknowledgment);

        ArgumentCaptor<StateTransitionEntity> saved = ArgumentCaptor.forClass(StateTransitionEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCardId()).isEqualTo(CARD_ID);
        assertThat(saved.getValue().getEvent()).isEqualTo("CREATE");
        assertThat(saved.getValue().getToState()).isEqualTo("REQUESTED");
        assertThat(saved.getValue().getMetadata()).contains("eventId");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldSkipDuplicateEventAndAcknowledge() throws Exception {
        when(repository.existsByCardIdAndEvent(CARD_ID, "CREATE")).thenReturn(true);

        consumer.consumeCardCreatedEvent(eventJson, 0, 1L, acknowledgment);

        verify(repository, never()).saveAndFlush(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldTreatConcurrentDuplicateAsProcessed() throws Exception {
        when(repository.existsByCardIdAndEvent(CARD_ID, "CREATE")).thenReturn(false, true);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_state_transitions_create"));

        consumer.consumeCardCreatedEvent(eventJson, 0, 2L, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldPropagateConstraintViolationThatIsNotADuplicate() {
        when(repository.existsByCardIdAndEvent(CARD_ID, "CREATE")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("fk_state_transitions_card"));

        assertThatThrownBy(() -> consumer.consumeCardCreatedEvent(eventJson, 0, 2L, acknowledgment))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldPropagateUnreadableMessageWithoutAcknowledging() {
        assertThatThrownBy(() -> consumer.consumeCardCreatedEvent("{not json", 0, 3L, acknowledgment))
                .isInstanceOf(JsonProcessingException.class);

        verify(repository, never()).saveAndFlush(any());
        verify(acknowledgment, never()).acknowledge();
    }
}
