package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.PIN;
import com.unisono.card.data.domain.port.input.ResetPINUseCase;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de PIN
 * Implementa los use cases de validación y reset de PIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PINManagementService implements ValidatePINUseCase, ResetPINUseCase {

    private final CardRepository cardRepository;
    private final EncryptionPort encryptionPort;

    // ========================================
    // ValidatePINUseCase
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public boolean execute(ValidatePINCommand command) {
        log.debug("Validating PIN for card: {}", command.cardId());

        Card card = getCardOrThrow(command.cardId());

        if (!card.isPINSet()) {
            log.warn("Card {} does not have a PIN set", command.cardId());
            return false;
        }

        // Validar PIN usando BCrypt
        boolean isValid = encryptionPort.verifyHash(command.plainPin(), card.getPinHash());

        if (isValid) {
            log.debug("PIN validated successfully for card: {}", command.cardId());
        } else {
            log.warn("Invalid PIN attempt for card: {}", command.cardId());
        }

        return isValid;
    }

    // ========================================
    // ResetPINUseCase
    // ========================================

    @Override
    public Card execute(ResetPINCommand command) {
        log.info("Resetting PIN for card: {}", command.cardId());

        Card card = getCardOrThrow(command.cardId());

        // Validar PIN actual (si está configurado)
        if (card.isPINSet()) {
            boolean isOldPINValid = encryptionPort.verifyHash(
                    command.oldPlainPin(),
                    card.getPinHash()
            );

            if (!isOldPINValid) {
                throw new BusinessRuleException("El PIN actual es incorrecto");
            }
        }

        // Validar formato del nuevo PIN
        PIN newPin = PIN.of(command.newPlainPin());

        // Hashear nuevo PIN
        String newPinHash = encryptionPort.hash(command.newPlainPin());

        // Resetear PIN en el aggregate
        card.resetPIN(newPinHash);

        Card savedCard = cardRepository.save(card);

        log.info("PIN reset successfully for card: {}", command.cardId());

        return savedCard;
    }

    // ========================================
    // Helper methods
    // ========================================

    private Card getCardOrThrow(CardId cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card", cardId.toString()));
    }
}
