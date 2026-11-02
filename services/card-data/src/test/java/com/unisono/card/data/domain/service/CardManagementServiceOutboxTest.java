package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.input.CreateCardUseCase.CreateCardCommand;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardManagementServiceOutboxTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EventOutboxPort eventOutbox;

    @Test
    void shouldEnqueueCardCreatedEventWhenCardIsCreated() {
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        CardManagementService service = new CardManagementService(cardRepository, eventOutbox);

        Card card = service.execute(new CreateCardCommand(
                ClientId.of("12345678"),
                CardHolder.create("Juan", "Pérez", "12345678", "juan@example.com",
                        "+5491123456789", LocalDate.of(1990, 1, 1)),
                CardType.VISA,
                CardCategory.GOLD
        ));

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventOutbox).enqueue(eq(KafkaTopics.CARD_CREATED), eq(card.getId().getValue()), event.capture());

        CardCreatedEvent created = (CardCreatedEvent) event.getValue();
        assertThat(created.getCardId()).isEqualTo(card.getId().getValue());
        assertThat(created.getClientId()).isEqualTo("12345678");
        assertThat(created.getHolderFullName()).isEqualTo("Juan Pérez");
        assertThat(created.getCardType()).isEqualTo("VISA");
        assertThat(created.getCardCategory()).isEqualTo("GOLD");
    }
}
