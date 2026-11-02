package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ClientId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CardPinAttemptsTest {

    @Test
    void shouldBlockCardAfterMaxFailedAttempts() {
        Card card = activeCard();

        assertThat(card.registerFailedPinAttempt()).isFalse();
        assertThat(card.registerFailedPinAttempt()).isFalse();
        assertThat(card.getRemainingPinAttempts()).isEqualTo(1);

        assertThat(card.registerFailedPinAttempt()).isTrue();
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(card.getBlockReason()).contains("intentos fallidos de PIN");
        assertThat(card.getRemainingPinAttempts()).isZero();
    }

    @Test
    void shouldResetCounterAfterCorrectPin() {
        Card card = activeCard();
        card.registerFailedPinAttempt();
        card.registerFailedPinAttempt();

        card.resetPinFailedAttempts();

        assertThat(card.getPinFailedAttempts()).isZero();
        assertThat(card.registerFailedPinAttempt()).isFalse();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void shouldResetCounterWhenUnblocked() {
        Card card = activeCard();
        for (int i = 0; i < Card.MAX_PIN_ATTEMPTS; i++) {
            card.registerFailedPinAttempt();
        }

        card.unblock();

        assertThat(card.getPinFailedAttempts()).isZero();
        assertThat(card.registerFailedPinAttempt()).isFalse();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void shouldResetCounterWhenPinChanges() {
        Card card = activeCard();
        card.registerFailedPinAttempt();

        card.resetPIN("$2a$10$newhash");

        assertThat(card.getPinFailedAttempts()).isZero();
    }

    @Test
    void shouldCountButNotBlockWhenStatusDoesNotAllowBlocking() {
        Card card = activeCard();
        card.cancel("Baja");

        for (int i = 0; i < Card.MAX_PIN_ATTEMPTS; i++) {
            assertThat(card.registerFailedPinAttempt()).isFalse();
        }

        assertThat(card.getPinFailedAttempts()).isEqualTo(Card.MAX_PIN_ATTEMPTS);
        assertThat(card.getStatus()).isEqualTo(CardStatus.CANCELLED);
    }

    private static Card activeCard() {
        CardHolder holder = CardHolder.create(
                "Juan",
                "Pérez",
                "12345678",
                "juan@example.com",
                "+5491123456789",
                LocalDate.of(1990, 1, 1)
        );
        Card card = Card.create(ClientId.of("12345678"), holder, CardType.VISA, CardCategory.CLASSIC);
        card.updateStatus(CardStatus.APPROVED);
        card.updateStatus(CardStatus.ISSUED);
        card.updateStatus(CardStatus.ACTIVE);
        return card;
    }
}
