package com.unisono.card.events.domain;

import java.time.LocalDateTime;

/**
 * Vista común de los eventos de cambio de estado de una tarjeta (card.approved, card.rejected, card.issued,
 * card.activated, card.blocked, card.unblocked, card.cancelled y card.expired).
 *
 * Los métodos no siguen la convención getX para que Jackson no los agregue al JSON: el contrato de cada evento
 * sigue siendo el de sus campos.
 */
public interface CardStatusChange {

    String getEventId();

    String getCardId();

    /** Estado anterior (nombre de CardStatus); nulo en mensajes publicados antes de que existiera el campo. */
    String getPreviousStatus();

    /** Estado al que pasó la tarjeta (nombre de CardStatus). */
    String newStatus();

    String getReason();

    /** Autor del cambio (approvedBy, blockedBy, etc.). */
    String actor();

    /** Cuándo ocurrió el cambio: la fecha propia del evento si la tiene, si no occurredOn. */
    LocalDateTime changedAt();
}
