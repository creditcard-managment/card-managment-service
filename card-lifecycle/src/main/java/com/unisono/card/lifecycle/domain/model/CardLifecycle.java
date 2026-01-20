package com.unisono.card.lifecycle.domain.model;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Gestiona las transiciones de estado del ciclo de vida de una tarjeta
 * Encapsula la lógica de negocio de cambios de estado
 */
@Slf4j
public class CardLifecycle {

    private final Card card;

    public CardLifecycle(Card card) {
        this.card = card;
    }

    /**
     * Aprueba una tarjeta (REQUESTED → APPROVED)
     */
    public void approve() {
        validateTransition(CardStatus.REQUESTED, CardStatus.APPROVED);

        card.setStatus(CardStatus.APPROVED);
        //card.setApprovedAt(LocalDateTime.now());

        log.debug("Card {} approved", card.getId());
    }

    /**
     * Rechaza una tarjeta (REQUESTED → REJECTED)
     */
    public void reject() {
        validateTransition(CardStatus.REQUESTED, CardStatus.REJECTED);

        card.setStatus(CardStatus.REJECTED);
        //card.setRejectedAt(LocalDateTime.now());

        log.debug("Card {} rejected", card.getId());
    }

    /**
     * Emite una tarjeta (APPROVED → ISSUED)
     */
    public void issue(CardNumber cardNumber, CVV cvv, String pinHash, ExpirationDate expirationDate) {
        validateTransition(CardStatus.APPROVED, CardStatus.ISSUED);

        card.setCardNumber(cardNumber);
        card.setCvv(cvv);
        card.setPinHash(pinHash);
        card.setExpirationDate(expirationDate);
        card.setStatus(CardStatus.ISSUED);
        card.setIssuedAt(LocalDateTime.now());

        log.debug("Card {} issued", card.getId());
    }

    /**
     * Activa una tarjeta (ISSUED → ACTIVE)
     */
    public void activate() {
        validateTransition(CardStatus.ISSUED, CardStatus.ACTIVE);

        card.setStatus(CardStatus.ACTIVE);
        card.setActivatedAt(LocalDateTime.now());

        log.debug("Card {} activated", card.getId());
    }

    /**
     * Bloquea una tarjeta (ACTIVE → BLOCKED)
     */
    public void block() {
        validateCanBlock();

        card.setStatus(CardStatus.BLOCKED);
        card.setBlockedAt(LocalDateTime.now());

        log.debug("Card {} blocked", card.getId());
    }

    /**
     * Bloquea una tarjeta por fraude (ACTIVE → BLOCKED_FRAUD)
     */
    public void blockForFraud() {
        validateCanBlock();

        card.setStatus(CardStatus.BLOCKED_FRAUD);
        card.setBlockedAt(LocalDateTime.now());

        log.debug("Card {} blocked for fraud", card.getId());
    }

    /**
     * Desbloquea una tarjeta (BLOCKED → ACTIVE)
     */
    public void unblock() {
        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new IllegalStateException(
                    String.format("Cannot unblock card in status %s", card.getStatus())
            );
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setBlockedAt(null);

        log.debug("Card {} unblocked", card.getId());
    }

    /**
     * Cancela una tarjeta (cualquier estado → CANCELLED)
     */
    public void cancel() {
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Card is already cancelled");
        }

        card.setStatus(CardStatus.CANCELLED);
        card.setCancelledAt(LocalDateTime.now());

        log.debug("Card {} cancelled", card.getId());
    }

    /**
     * Valida que se pueda bloquear la tarjeta
     */
    private void validateCanBlock() {
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot block a cancelled card");
        }
        if (card.getStatus() == CardStatus.BLOCKED || card.getStatus() == CardStatus.BLOCKED_FRAUD) {
            throw new IllegalStateException("Card is already blocked");
        }
    }

    /**
     * Valida que la transición sea válida
     */
    private void validateTransition(CardStatus expectedCurrent, CardStatus target) {
        if (card.getStatus() != expectedCurrent) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s. Current status must be %s",
                            card.getStatus(), target, expectedCurrent)
            );
        }
    }

    /**
     * Obtiene la tarjeta gestionada
     */
    public Card getCard() {
        return card;
    }
}
