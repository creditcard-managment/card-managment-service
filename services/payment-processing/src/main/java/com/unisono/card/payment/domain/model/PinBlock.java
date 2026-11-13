package com.unisono.card.payment.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;

/**
 * PIN cifrado por el terminal en un PIN block ISO 9564 formato 4 (AES), con el id de la clave usada.
 * payment-processing no puede descifrarlo: lo reenvía a card-data, que tiene la clave y el PAN.
 *
 * @param block bloque en hexadecimal (32 caracteres)
 * @param keyId id de la clave de cifrado de PIN (PEK) del terminal
 */
public record PinBlock(String block, String keyId) {

    public PinBlock {
        if (block == null || !block.matches("[0-9A-Fa-f]{32}")) {
            throw new ValidationException("PIN block must be 32 hexadecimal characters");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new ValidationException("PIN key id is required");
        }
    }

    @Override
    public String toString() {
        return "PinBlock[keyId=" + keyId + "]";
    }
}
