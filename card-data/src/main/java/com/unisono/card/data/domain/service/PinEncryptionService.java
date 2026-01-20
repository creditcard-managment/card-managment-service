package com.unisono.card.data.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de dominio para manejar la encriptación del PIN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PinEncryptionService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Encripta un PIN en texto plano
     */
    public String encrypt(String plainPin) {
        validatePin(plainPin);
        return passwordEncoder.encode(plainPin);
    }

    /**
     * Verifica si un PIN en texto plano coincide con el hash
     */
    public boolean matches(String plainPin, String hashedPin) {
        validatePin(plainPin);
        return passwordEncoder.matches(plainPin, hashedPin);
    }

    /**
     * Valida el formato del PIN
     */
    private void validatePin(String pin) {
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
    }
}
