package com.unisono.card.shared.domain.exception;

/**
 * Excepción lanzada cuando un Value Object no pasa su validación.
 * Extiende ValidationException para que los servicios la traten como un dato
 * de entrada inválido (400).
 */
public class InvalidValueObjectException extends ValidationException {

    private static final long serialVersionUID = 1L;

    public InvalidValueObjectException(String message) {
        super(message);
    }

    public InvalidValueObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
