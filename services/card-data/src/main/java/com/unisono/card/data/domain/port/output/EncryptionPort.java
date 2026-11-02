package com.unisono.card.data.domain.port.output;

/**
 * Port de salida: Encriptación de datos sensibles
 *
 * Será implementado por el adaptador de encriptación (usa el servicio de card-shared).
 */
public interface EncryptionPort {

    /**
     * Encripta datos sensibles (PAN, CVV)
     */
    String encrypt(String plainText);

    /**
     * Desencripta datos sensibles
     */
    String decrypt(String encryptedText);

    /**
     * Hashea un valor (PIN)
     */
    String hash(String plainValue);

    /**
     * Verifica un hash
     */
    boolean verifyHash(String plainValue, String hashedValue);
}
