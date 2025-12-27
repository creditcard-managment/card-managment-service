package com.unisono.card.shared.domain.exception;


/**
 * Excepción lanzada cuando falla una validación de negocio.
 */
public class ValidationException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
