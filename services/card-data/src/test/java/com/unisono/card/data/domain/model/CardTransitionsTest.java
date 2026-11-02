package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTransitionsTest {

    @Test
    void shouldFollowIssuanceFlow() {
        Card card = newCard();

        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        card.updateStatus(CardStatus.ACTIVE);

        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void shouldRejectSkippingStates() {
        Card card = newCard();

        assertThatThrownBy(() -> card.updateStatus(CardStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REQUESTED → ACTIVE");
        assertThat(card.getStatus()).isEqualTo(CardStatus.REQUESTED);
    }

    @Test
    void shouldRejectBlockingRequestedCard() {
        Card card = newCard();

        assertThatThrownBy(() -> card.block("Pérdida"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> card.blockForFraud("Fraude"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(card.getBlockedAt()).isNull();
    }

    @Test
    void shouldBlockAndUnblockActiveCard() {
        Card card = activeCard();

        card.block("Pérdida");
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(card.getBlockReason()).isEqualTo("Pérdida");
        assertThat(card.getBlockedAt()).isNotNull();

        card.unblock();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getBlockReason()).isNull();
        assertThat(card.getBlockedAt()).isNull();
    }

    @Test
    void shouldRejectUnblockingCardThatIsNotBlocked() {
        Card card = activeCard();

        assertThatThrownBy(card::unblock)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está bloqueada");
    }

    @Test
    void shouldNotChangeCancelledCard() {
        Card card = activeCard();
        card.cancel("Baja");

        assertThatThrownBy(() -> card.cancel("Otra baja")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> card.updateStatus(CardStatus.ACTIVE)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> card.block("Pérdida")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(card::expire).isInstanceOf(BusinessRuleException.class);
        assertThat(card.getStatus()).isEqualTo(CardStatus.CANCELLED);
    }

    @Test
    void shouldExpireActiveCard() {
        Card card = activeCard();

        card.expire();

        assertThat(card.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(card.getExpiredAt()).isNotNull();
    }

    private static Card activeCard() {
        Card card = newCard();
        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        card.updateStatus(CardStatus.ACTIVE);
        return card;
    }

    private static Card newCard() {
        CardHolder holder = CardHolder.create(
                "Juan",
                "Pérez",
                "12345678",
                "juan@example.com",
                "+5491123456789",
                LocalDate.of(1990, 1, 1)
        );
        return Card.create(ClientId.of("12345678"), holder, CardType.VISA, CardCategory.CLASSIC);
    }
}
