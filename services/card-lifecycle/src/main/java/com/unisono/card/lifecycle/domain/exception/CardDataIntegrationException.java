package com.unisono.card.lifecycle.domain.exception;

import com.unisono.card.shared.domain.exception.DomainException;

/**
 * card-data no respondió o respondió con un error que no corresponde a una regla de negocio.
 */
public class CardDataIntegrationException extends DomainException {

    public CardDataIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
