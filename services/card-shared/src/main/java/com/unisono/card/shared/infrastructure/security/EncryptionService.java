package com.unisono.card.shared.infrastructure.security;

/**
 * Servicio de encriptación para datos sensibles.
 * Implementación usa AES-256-GCM con claves versionadas.
 */
public interface EncryptionService {

    /**
     * Encripta un texto plano con la clave activa
     * @param plainText texto a encriptar
     * @return "keyId:Base64(IV + ciphertext)"
     */
    String encrypt(String plainText);

    /**
     * Desencripta con la clave indicada en el prefijo (o la legacy si no tiene prefijo)
     * @param encryptedText texto devuelto por encrypt
     * @return texto plano original
     */
    String decrypt(String encryptedText);
}
