package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.model.CardType;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EventOutboxPort;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardExpiredEvent;
import com.unisono.card.events.domain.DomainEvent;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.PIN;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardExpirationServiceTest {

    private static final YearMonth DECEMBER = YearMonth.of(2026, 12);

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EventOutboxPort eventOutbox;

    @Test
    void expiresEachLockedCardAndEnqueuesCardExpired() {
        Card active = issuedCard(11, 2026);
        active.updateStatus(CardStatus.ACTIVE);
        Card blocked = issuedCard(10, 2026);
        blocked.block("Pérdida");
        when(cardRepository.lockExpiredBatch(DECEMBER, 50)).thenReturn(List.of(active, blocked));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        List<CardId> expired = service().expireBatch(DECEMBER, 50);

        assertThat(expired).containsExactly(active.getId(), blocked.getId());
        assertThat(active.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(blocked.getStatus()).isEqualTo(CardStatus.EXPIRED);
        verify(cardRepository, times(2)).save(any(Card.class));

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventOutbox).enqueue(eq(KafkaTopics.CARD_EXPIRED), eq(active.getId().getValue()), event.capture());
        CardExpiredEvent cardExpired = (CardExpiredEvent) event.getValue();
        assertThat(cardExpired.getCardId()).isEqualTo(active.getId().getValue());
        assertThat(cardExpired.getExpiredAt()).isEqualTo(active.getExpiredAt());
        assertThat(cardExpired.getReason()).isEqualTo("Vencimiento por fecha (11/26)");
        assertThat(cardExpired.getExpiredBy()).isEqualTo("system");
        verify(eventOutbox).enqueue(eq(KafkaTopics.CARD_EXPIRED), eq(blocked.getId().getValue()), any());
    }

    @Test
    void nothingToExpire() {
        when(cardRepository.lockExpiredBatch(DECEMBER, 50)).thenReturn(List.of());

        assertThat(service().expireBatch(DECEMBER, 50)).isEmpty();
        verify(cardRepository, never()).save(any());
        verify(eventOutbox, never()).enqueue(any(), any(), any());
    }

    private CardExpirationService service() {
        return new CardExpirationService(cardRepository, new CardEventEmitter(eventOutbox));
    }

    private static Card issuedCard(int month, int year) {
        Card card = Card.create(ClientId.of("12345678"),
                CardHolder.create("Juan", "Pérez", "12345678", "juan@example.com", "+5491123456789",
                        LocalDate.of(1990, 1, 1)),
                CardType.VISA, CardCategory.CLASSIC);
        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        card.updateCardData(CardNumber.of("4532015112830366"), CVV.of("123"), PIN.of("1357"),
                ExpirationDate.of(month, year));
        return card;
    }
}
