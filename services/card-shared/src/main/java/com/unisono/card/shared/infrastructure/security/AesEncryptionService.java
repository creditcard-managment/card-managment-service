package com.unisono.card.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementación de EncryptionService usando AES-256-GCM con un llavero de claves versionadas
 *
 * Formato: "keyId:Base64(IV ‖ ciphertext+tag)". El keyId indica con qué clave se descifra,
 * así se puede rotar la clave activa sin perder los datos cifrados con las anteriores.
 * Los datos sin prefijo (formato anterior) se descifran con legacy-secret-key.
 *
 * IMPORTANTE: En producción, las claves deben venir de un Key Management Service (KMS)
 * como AWS KMS, Azure Key Vault, HashiCorp Vault, etc.
 */
@Slf4j
@Service
public class AesEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 32;
    private static final char SEPARATOR = ':';
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final String activeKeyId;
    private final Map<String, SecretKey> keys;
    private final SecretKey legacyKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionService(EncryptionProperties properties) {
        this.keys = loadKeys(properties.getKeys());
        this.activeKeyId = properties.getActiveKeyId();

        if (activeKeyId == null || activeKeyId.isBlank()) {
            throw new IllegalStateException("encryption.active-key-id is required");
        }
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalStateException(
                    "encryption.active-key-id '" + activeKeyId + "' is not defined in encryption.keys " + keys.keySet()
            );
        }

        this.legacyKey = loadLegacyKey(properties.getLegacySecretKey());

        log.info("🔐 AesEncryptionService initialized: active key '{}', keys {}, legacy key {}",
                activeKeyId, keys.keySet(), legacyKey != null ? "configured" : "not configured");
        log.warn("⚠️ Encryption keys come from configuration. IMPORTANT: Use KMS in production!");
    }

    private static Map<String, SecretKey> loadKeys(Map<String, String> configured) {
        Map<String, SecretKey> result = new LinkedHashMap<>();
        configured.forEach((id, value) -> {
            if (!KEY_ID_PATTERN.matcher(id).matches()) {
                throw new IllegalStateException(
                        "Invalid encryption key id '" + id + "': use letters, digits, '_' or '-' (max 32)"
                );
            }

            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(value == null ? "" : value.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("encryption.keys." + id + " is not valid Base64", e);
            }
            if (keyBytes.length != KEY_LENGTH) {
                throw new IllegalStateException(
                        "encryption.keys." + id + " must decode to exactly 32 bytes (256 bits). Current length: "
                                + keyBytes.length + ". Generate one with: openssl rand -base64 32"
                );
            }

            result.put(id, new SecretKeySpec(keyBytes, "AES"));
        });
        return Map.copyOf(result);
    }

    private static SecretKey loadLegacyKey(String legacySecretKey) {
        if (legacySecretKey == null || legacySecretKey.isEmpty()) {
            return null;
        }

        byte[] keyBytes = legacySecretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "encryption.legacy-secret-key must be exactly 32 bytes (256 bits). Current length: " + keyBytes.length
            );
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        byte[] encrypted = encrypt(plainText.getBytes(StandardCharsets.UTF_8), keys.get(activeKeyId));
        return activeKeyId + SEPARATOR + Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        int separator = encryptedText.indexOf(SEPARATOR);
        SecretKey key;
        String payload;

        if (separator < 0) {
            // Formato anterior, sin keyId (Base64 no contiene ':')
            if (legacyKey == null) {
                throw new EncryptionException(
                        "Encrypted value has no key id and encryption.legacy-secret-key is not configured"
                );
            }
            key = legacyKey;
            payload = encryptedText;
        } else {
            String keyId = encryptedText.substring(0, separator);
            key = keys.get(keyId);
            if (key == null) {
                throw new EncryptionException(
                        "Unknown encryption key id '" + keyId + "'. Configured keys: " + keys.keySet()
                );
            }
            payload = encryptedText.substring(separator + 1);
        }

        try {
            byte[] decrypted = decrypt(Base64.getDecoder().decode(payload), key);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Encrypted value is not valid Base64", e);
        }
    }

    private byte[] encrypt(byte[] plainBytes, SecretKey key) {
        try {
            // Generar IV aleatorio
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainBytes);

            // Combinar IV + cipherText
            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();

        } catch (Exception e) {
            log.error("Error encrypting data: {}", e.getClass().getSimpleName());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    private byte[] decrypt(byte[] encryptedBytes, SecretKey key) {
        if (encryptedBytes.length <= GCM_IV_LENGTH) {
            throw new EncryptionException("Encrypted value is too short");
        }

        try {
            // Extraer IV y cipherText
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(cipherText);

        } catch (Exception e) {
            log.error("Error decrypting data: {}", e.getClass().getSimpleName());
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
}
