package com.unisono.card.shared.infrastructure.security;

/**
 * Excepción lanzada cuando hay errores de hashing
 */
public class HashingException extends RuntimeException {

    public HashingException(String message) {
        super(message);
    }

    public HashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
