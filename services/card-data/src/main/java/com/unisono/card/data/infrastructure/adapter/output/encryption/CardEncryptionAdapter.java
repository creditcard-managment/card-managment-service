package com.unisono.card.data.infrastructure.adapter.output.encryption;

import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.shared.infrastructure.security.EncryptionService;
import com.unisono.card.shared.infrastructure.security.HashingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptador de encriptación
 *
 * Implementa EncryptionPort usando los servicios de card-shared
 */
@Component
@RequiredArgsConstructor
public class CardEncryptionAdapter implements EncryptionPort {

    private final EncryptionService encryptionService;
    private final HashingService hashingService;

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        return encryptionService.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        return encryptionService.decrypt(encryptedText);
    }

    @Override
    public String hash(String plainValue) {
        if (plainValue == null || plainValue.isEmpty()) {
            return null;
        }
        return hashingService.hash(plainValue);
    }

    @Override
    public boolean verifyHash(String plainValue, String hashedValue) {
        if (plainValue == null || hashedValue == null) {
            return false;
        }
        return hashingService.verify(plainValue, hashedValue);
    }
}
