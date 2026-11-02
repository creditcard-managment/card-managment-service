package com.unisono.card.shared.domain.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 */
public class BusinessRuleException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
