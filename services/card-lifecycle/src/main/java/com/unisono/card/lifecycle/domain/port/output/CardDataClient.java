package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.shared.domain.valueobject.*;

import java.time.LocalDateTime;

/**
 * Puerto de salida hacia card-data (dueño de la tarjeta).
 *
 * Las implementaciones traducen los errores HTTP a excepciones de dominio:
 * 404 → NotFoundException, 400 → ValidationException,
 * 409/422 → BusinessRuleException, resto → CardDataIntegrationException.
 */
public interface CardDataClient {

    CardStatus getStatus(CardId cardId);

    void updateStatus(CardId cardId, String status);

    /** Bloqueo temporal (POST /block): card-data registra block_reason y blocked_at. */
    void block(CardId cardId, String reason);

    /** Bloqueo por fraude (POST /block-fraud). */
    void blockForFraud(CardId cardId, String reason);

    /** Cancelación (POST /cancel): card-data registra cancelled_reason y cancelled_at. */
    void cancel(CardId cardId, String reason);

    void updateActivatedAt(CardId cardId, LocalDateTime activatedAt);

    void updateCardData(
            CardId cardId,
            CardNumber cardNumber,
            CVV cvv,
            PIN pin,
            ExpirationDate expirationDate
    );

    /**
     * Verifica el PIN contra el hash guardado en card-data.
     * Un PIN incorrecto suma un intento fallido en card-data.
     */
    boolean validatePin(CardId cardId, PIN pin);
}
