package com.unisono.card.shared.domain.exception;

/**
 * Excepción base del dominio.
 * Todas las excepciones de negocio heredan de esta.
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
