package com.unisono.card.shared.infrastructure.security;

/**
 * Excepción lanzada cuando hay errores de encriptación/desencriptación
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
