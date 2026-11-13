package com.unisono.card.shared.infrastructure.security;

import com.unisono.card.shared.domain.exception.ValidationException;

/**
 * El PIN block no se puede descifrar: formato inválido, clave equivocada o bloque de otra tarjeta.
 * Nunca incluye el PIN ni el bloque en el mensaje.
 */
public class InvalidPinBlockException extends ValidationException {

    public InvalidPinBlockException(String message) {
        super(message);
    }
}
