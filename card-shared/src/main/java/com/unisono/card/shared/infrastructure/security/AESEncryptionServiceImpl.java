package com.unisono.card.shared.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementación de encriptación usando AES-256-GCM.
 * GCM proporciona autenticación además de confidencialidad.
 */
@Service
public class AESEncryptionServiceImpl implements EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AESEncryptionServiceImpl(
            @Value("${security.encryption.key:MySecretKey32BytesForAES256Key!!}") String key) {
        // En producción, la key debe venir de un vault (AWS Secrets Manager, HashiCorp Vault, etc)
        if (key.length() != 32) {
            throw new IllegalArgumentException("La clave debe tener exactamente 32 bytes para AES-256");
        }
        this.secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] encrypted = encrypt(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = decrypt(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al desencriptar", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] plainBytes) {
        try {
            // Generar IV aleatorio
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainBytes);

            // Concatenar IV + cipherText
            byte[] encrypted = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, encrypted, GCM_IV_LENGTH, cipherText.length);

            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar bytes", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedBytes) {
        try {
            // Extraer IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);

            // Extraer cipherText
            byte[] cipherText = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Error al desencriptar bytes", e);
        }
    }
}
