package com.unisono.card.data.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;

/**
 * Use Case: Validar PIN de una tarjeta
 */
public interface ValidatePINUseCase {

    /**
     * Valida si el PIN proporcionado es correcto
     *
     * @param command datos para validar PIN
     * @return true si el PIN es correcto
     */
    boolean execute(ValidatePINCommand command);

    /**
     * Command para validar PIN. Lleva el PIN cifrado en un PIN block (pinBlock + pinKeyId) o, solo para
     * los clientes internos que todavía no cifran (activación desde card-lifecycle), el PIN en claro.
     */
    record ValidatePINCommand(
            CardId cardId,
            String plainPin,
            String pinBlock,
            String pinKeyId
    ) {
        public ValidatePINCommand(CardId cardId, String plainPin) {
            this(cardId, plainPin, null, null);
        }

        public static ValidatePINCommand ofPinBlock(CardId cardId, String pinBlock, String pinKeyId) {
            return new ValidatePINCommand(cardId, null, pinBlock, pinKeyId);
        }

        public boolean hasPinBlock() {
            return pinBlock != null;
        }

        @Override
        public String toString() {
            return "ValidatePINCommand[cardId=" + cardId + ", pinBlock=" + hasPinBlock() + ", pinKeyId=" + pinKeyId + "]";
        }
    }
}
