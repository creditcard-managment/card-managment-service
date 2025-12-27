package com.unisono.card.shared.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementación de hashing usando BCrypt.
 * BCrypt es resistente a ataques de fuerza bruta por su salt y trabajo adaptativo.
 */
@Service
public class BCryptHashingServiceImpl implements HashingService {

    private static final int STRENGTH = 12; // Factor de trabajo (2^12 iteraciones)

    private final BCryptPasswordEncoder encoder;

    public BCryptHashingServiceImpl() {
        this.encoder = new BCryptPasswordEncoder(STRENGTH);
    }

    @Override
    public String hash(String plainValue) {
        if (plainValue == null || plainValue.isEmpty()) {
            throw new IllegalArgumentException("El valor a hashear no puede ser nulo o vacío");
        }
        return encoder.encode(plainValue);
    }

    @Override
    public boolean verify(String plainValue, String hashedValue) {
        if (plainValue == null || hashedValue == null) {
            return false;
        }
        return encoder.matches(plainValue, hashedValue);
    }
}
