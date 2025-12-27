package com.unisono.card.shared.infrastructure.security;

/**
 * Servicio de encriptación para datos sensibles.
 * Implementación usa AES-256.
 */
public interface EncryptionService {

    /**
     * Encripta un texto plano
     * @param plainText texto a encriptar
     * @return texto encriptado en Base64
     */
    String encrypt(String plainText);

    /**
     * Desencripta un texto encriptado
     * @param encryptedText texto encriptado en Base64
     * @return texto plano original
     */
    String decrypt(String encryptedText);

    /**
     * Encripta bytes
     */
    byte[] encrypt(byte[] plainBytes);

    /**
     * Desencripta bytes
     */
    byte[] decrypt(byte[] encryptedBytes);
}
