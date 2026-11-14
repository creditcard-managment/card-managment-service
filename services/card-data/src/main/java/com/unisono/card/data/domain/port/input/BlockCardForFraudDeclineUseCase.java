package com.unisono.card.data.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;

import java.time.LocalDateTime;

/**
 * Port de entrada: bloqueo automático de una tarjeta cuando payment-processing rechaza un pago por fraude
 */
public interface BlockCardForFraudDeclineUseCase {

    enum Outcome {
        /** La tarjeta pasó a BLOCKED_FRAUD */
        BLOCKED,
        /** Ya estaba bloqueada por fraude, o en un estado que no se puede bloquear (bloqueada, cancelada…) */
        NOT_BLOCKABLE,
        /** La tarjeta no existe */
        CARD_NOT_FOUND,
        /** El evento ya se había procesado */
        DUPLICATE
    }

    /**
     * @param eventId id del evento payment.processing.declined (deduplicación)
     */
    record FraudDeclineCommand(
            String eventId,
            CardId cardId,
            String transactionId,
            String declineReason,
            LocalDateTime declinedAt
    ) {}

    Outcome execute(FraudDeclineCommand command);
}
