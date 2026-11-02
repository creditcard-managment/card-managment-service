package com.unisono.card.shared.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionServiceTest {

    private static final String K1 = randomKey();
    private static final String K2 = randomKey();
    private static final String LEGACY = "MySecretKey32BytesForAES256!!123";
    private static final String PAN = "4532015112830366";

    @Test
    void shouldEncryptWithActiveKeyPrefixAndDecrypt() {
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);

        String encrypted = service.encrypt(PAN);

        assertThat(encrypted).startsWith("k1:").doesNotContain(PAN);
        assertThat(service.decrypt(encrypted)).isEqualTo(PAN);
    }

    @Test
    void shouldUseRandomIvPerEncryption() {
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);

        assertThat(service.encrypt(PAN)).isNotEqualTo(service.encrypt(PAN));
    }

    @Test
    void shouldDecryptValuesFromPreviousKeyAfterRotation() {
        String encryptedWithK1 = service("k1", Map.of("k1", K1), null).encrypt(PAN);

        AesEncryptionService rotated = service("k2", Map.of("k1", K1, "k2", K2), null);

        assertThat(rotated.decrypt(encryptedWithK1)).isEqualTo(PAN);
        assertThat(rotated.encrypt(PAN)).startsWith("k2:");
    }

    @Test
    void shouldDecryptLegacyValuesWithoutPrefix() throws Exception {
        String legacyValue = legacyEncrypt(PAN, LEGACY);
        AesEncryptionService service = service("k1", Map.of("k1", K1), LEGACY);

        assertThat(service.decrypt(legacyValue)).isEqualTo(PAN);
    }

    @Test
    void shouldFailOnLegacyValueWithoutLegacyKey() throws Exception {
        String legacyValue = legacyEncrypt(PAN, LEGACY);
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);

        assertThatThrownBy(() -> service.decrypt(legacyValue))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("legacy-secret-key");
    }

    @Test
    void shouldFailOnUnknownKeyId() {
        String encryptedWithK2 = service("k2", Map.of("k2", K2), null).encrypt(PAN);
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);

        assertThatThrownBy(() -> service.decrypt(encryptedWithK2))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("k2");
    }

    @Test
    void shouldFailWhenKeyMaterialChanged() {
        String encrypted = service("k1", Map.of("k1", K1), null).encrypt(PAN);
        AesEncryptionService otherMaterial = service("k1", Map.of("k1", K2), null);

        assertThatThrownBy(() -> otherMaterial.decrypt(encrypted))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void shouldFailWhenCiphertextIsTampered() {
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);
        String encrypted = service.encrypt(PAN);
        byte[] bytes = Base64.getDecoder().decode(encrypted.substring(3));
        bytes[bytes.length - 1] ^= 1;

        assertThatThrownBy(() -> service.decrypt("k1:" + Base64.getEncoder().encodeToString(bytes)))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void shouldReturnNullForEmptyValues() {
        AesEncryptionService service = service("k1", Map.of("k1", K1), null);

        assertThat(service.encrypt(null)).isNull();
        assertThat(service.encrypt("")).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void shouldRequireActiveKeyId() {
        assertThatThrownBy(() -> service(null, Map.of("k1", K1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active-key-id");
    }

    @Test
    void shouldRequireActiveKeyToBeDefined() {
        assertThatThrownBy(() -> service("k9", Map.of("k1", K1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("k9");
    }

    @Test
    void shouldRejectKeyThatIsNot32Bytes() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> service("k1", Map.of("k1", shortKey), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void shouldRejectKeyThatIsNotBase64() {
        assertThatThrownBy(() -> service("k1", Map.of("k1", "not base64!!"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void shouldRejectInvalidKeyId() {
        assertThatThrownBy(() -> service("k:1", Map.of("k:1", K1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key id");
    }

    @Test
    void shouldRejectLegacyKeyThatIsNot32Bytes() {
        assertThatThrownBy(() -> service("k1", Map.of("k1", K1), "short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("legacy-secret-key");
    }

    private static AesEncryptionService service(String activeKeyId, Map<String, String> keys, String legacy) {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setActiveKeyId(activeKeyId);
        properties.setKeys(keys);
        properties.setLegacySecretKey(legacy);
        return new AesEncryptionService(properties);
    }

    private static String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * Formato anterior al llavero: Base64(IV ‖ ciphertext) sin prefijo, clave = bytes UTF-8 del String
     */
    private static String legacyEncrypt(String plain, String secret) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array());
    }
}
