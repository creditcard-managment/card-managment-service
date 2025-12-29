package com.unisono.card.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementación de EncryptionService usando AES-256-GCM
 *
 * IMPORTANTE: En producción, la clave debe estar en un Key Management Service (KMS)
 * como AWS KMS, Azure Key Vault, HashiCorp Vault, etc.
 */
@Slf4j
@Service
public class AesEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesEncryptionService(
            @Value("${encryption.secret-key:MySecretKey32BytesForAES256!!}") String secretKeyString
    ) {
        // Validar que la clave tenga 32 bytes (256 bits)
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Encryption key must be exactly 32 bytes (256 bits). Current length: " + keyBytes.length
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();

        log.warn("⚠️ AesEncryptionService initialized. IMPORTANT: Use KMS in production!");
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            byte[] encrypted = encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error encrypting string", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting string", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] plainBytes) {
        if (plainBytes == null || plainBytes.length == 0) {
            return null;
        }

        try {
            // Generar IV aleatorio
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Configurar cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encriptar
            byte[] cipherText = cipher.doFinal(plainBytes);

            // Combinar IV + cipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return byteBuffer.array();

        } catch (Exception e) {
            log.error("Error encrypting bytes", e);
            throw new EncryptionException("Failed to encrypt bytes", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedBytes) {
        if (encryptedBytes == null || encryptedBytes.length == 0) {
            return null;
        }

        try {
            // Extraer IV y cipherText
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // Configurar cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Desencriptar
            return cipher.doFinal(cipherText);

        } catch (Exception e) {
            log.error("Error decrypting bytes", e);
            throw new EncryptionException("Failed to decrypt bytes", e);
        }
    }
}
