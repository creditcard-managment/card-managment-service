package com.unisono.card.shared.infrastructure.security;

/**
 * Servicio de hashing para passwords/PINs.
 * Implementación usa BCrypt.
 */
public interface HashingService {

    /**
     * Hashea un valor (ej: PIN)
     * @param plainValue valor en texto plano
     * @return hash BCrypt
     */
    String hash(String plainValue);

    /**
     * Verifica si un valor coincide con un hash
     * @param plainValue valor en texto plano
     * @param hashedValue hash BCrypt
     * @return true si coincide
     */
    boolean verify(String plainValue, String hashedValue);
}
