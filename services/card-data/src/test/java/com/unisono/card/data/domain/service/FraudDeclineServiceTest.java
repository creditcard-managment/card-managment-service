package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.FraudDeclineCommand;
import com.unisono.card.data.domain.port.input.BlockCardForFraudDeclineUseCase.Outcome;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.data.domain.port.output.ProcessedEventPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardBlockedEvent;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDeclineServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ProcessedEventPort processedEvents;

    @Mock
    private EventOutboxPort eventOutbox;

    private FraudDeclineService service;

    @BeforeEach
    void setUp() {
        service = new FraudDeclineService(cardRepository, processedEvents, new CardEventEmitter(eventOutbox));
        lenient().when(processedEvents.markProcessed(any(), eq(KafkaTopics.PAYMENT_DECLINED))).thenReturn(true);
        lenient().when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void blocksAnActiveCardForFraudAndPublishesCardBlocked() {
        Card card = card(CardStatus.ACTIVE);
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        Outcome outcome = service.execute(decline(card.getId()));

        assertThat(outcome).isEqualTo(Outcome.BLOCKED);
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED_FRAUD);
        assertThat(card.getBlockReason()).contains("tx-1").contains("Riesgo alto");

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventOutbox).enqueue(eq(KafkaTopics.CARD_BLOCKED), eq(card.getId().getValue()), event.capture());
        CardBlockedEvent blocked = (CardBlockedEvent) event.getValue();
        assertThat(blocked.getBlockType()).isEqualTo("FRAUD");
        assertThat(blocked.getBlockedBy()).isEqualTo("system");
        assertThat(blocked.getPreviousStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void blocksAnIssuedCardThatWasNeverActivated() {
        Card card = card(CardStatus.ISSUED);
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        assertThat(service.execute(decline(card.getId()))).isEqualTo(Outcome.BLOCKED);
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED_FRAUD);
    }

    @Test
    void cardAlreadyBlockedIsLeftAsIs() {
        Card card = card(CardStatus.ACTIVE);
        card.block("Pérdida");
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        assertThat(service.execute(decline(card.getId()))).isEqualTo(Outcome.NOT_BLOCKABLE);
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(eventOutbox);
    }

    @Test
    void duplicateEventDoesNothing() {
        CardId cardId = CardId.generate();
        when(processedEvents.markProcessed("evt-1", KafkaTopics.PAYMENT_DECLINED)).thenReturn(false);

        assertThat(service.execute(decline(cardId))).isEqualTo(Outcome.DUPLICATE);
        verifyNoInteractions(cardRepository, eventOutbox);
    }

    @Test
    void unknownCardIsRecordedAndSkipped() {
        CardId cardId = CardId.generate();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThat(service.execute(decline(cardId))).isEqualTo(Outcome.CARD_NOT_FOUND);
        verifyNoInteractions(eventOutbox);
    }

    private static FraudDeclineCommand decline(CardId cardId) {
        return new FraudDeclineCommand("evt-1", cardId, "tx-1", "Riesgo alto", LocalDateTime.now());
    }

    private static Card card(CardStatus status) {
        Card card = Card.create(ClientId.of("12345678"),
                CardHolder.create("Juan", "Pérez", "12345678", "juan@example.com", "+5491123456789",
                        LocalDate.of(1990, 1, 1)),
                CardType.VISA, CardCategory.CLASSIC);
        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        if (status == CardStatus.ACTIVE) {
            card.updateStatus(CardStatus.ACTIVE);
        }
        return card;
    }
}
