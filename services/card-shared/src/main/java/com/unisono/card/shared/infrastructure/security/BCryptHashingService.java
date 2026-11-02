package com.unisono.card.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementación de HashingService usando BCrypt
 * BCrypt es ideal para hashear passwords y PINs
 */
@Slf4j
@Service
public class BCryptHashingService implements HashingService {

    private final BCryptPasswordEncoder encoder;

    public BCryptHashingService() {
        // Strength 12 es un buen balance entre seguridad y performance
        this.encoder = new BCryptPasswordEncoder(12);
        log.info("✅ BCryptHashingService initialized with strength 12");
    }

    @Override
    public String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Cannot hash null or empty value");
        }

        try {
            String hashed = encoder.encode(plainText);
            log.debug("Value hashed successfully");
            return hashed;
        } catch (Exception e) {
            log.error("Error hashing value", e);
            throw new HashingException("Failed to hash value", e);
        }
    }

    @Override
    public boolean verify(String plainText, String hashedValue) {
        if (plainText == null || hashedValue == null) {
            return false;
        }

        try {
            boolean matches = encoder.matches(plainText, hashedValue);
            log.debug("Verification result: {}", matches);
            return matches;
        } catch (Exception e) {
            log.error("Error verifying value", e);
            return false;
        }
    }
}
