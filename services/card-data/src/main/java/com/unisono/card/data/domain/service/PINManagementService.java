package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.shared.domain.valueobject.PIN;
import com.unisono.card.data.domain.port.input.ResetPINUseCase;
import com.unisono.card.data.domain.port.input.ValidatePINUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.domain.port.output.EncryptionPort;
import com.unisono.card.data.domain.port.output.PinBlockPort;
import com.unisono.card.shared.infrastructure.security.InvalidPinBlockException;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de PIN
 * Implementa los use cases de validación y reset de PIN.
 *
 * Cada PIN incorrecto suma un intento fallido; al llegar a Card.MAX_PIN_ATTEMPTS
 * la tarjeta se bloquea. Un PIN correcto reinicia el contador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PINManagementService implements ValidatePINUseCase, ResetPINUseCase {

    private final CardRepository cardRepository;
    private final EncryptionPort encryptionPort;
    private final CardEventEmitter eventEmitter;
    private final PinBlockPort pinBlockPort;

    // ========================================
    // ValidatePINUseCase
    // ========================================

    @Override
    public boolean execute(ValidatePINCommand command) {
        log.debug("Validating PIN for card: {}", command.cardId());

        Card card = getCardOrThrow(command.cardId());
        ensureNotBlocked(card);

        if (!card.isPINSet()) {
            log.warn("Card {} does not have a PIN set", command.cardId());
            return false;
        }

        // Un PIN block se descifra solo en memoria. Si no se puede descifrar (clave, formato o tarjeta
        // equivocados) se rechaza con 400 sin contar un intento: no se llegó a comparar ningún PIN
        String pin = command.hasPinBlock() ? decryptPinBlock(command, card) : command.plainPin();

        // Validar PIN usando BCrypt
        boolean isValid = encryptionPort.verifyHash(pin, card.getPinHash());

        if (isValid) {
            card.resetPinFailedAttempts();
            log.debug("PIN validated successfully for card: {}", command.cardId());
        } else {
            registerFailedAttempt(card);
        }

        cardRepository.save(card);

        return isValid;
    }

    private String decryptPinBlock(ValidatePINCommand command, Card card) {
        if (card.getCardNumber() == null) {
            // Sin PAN no se puede descifrar: el bloque está atado a él
            throw new InvalidPinBlockException("La tarjeta no tiene PAN para descifrar el PIN block");
        }
        return pinBlockPort.decryptPin(command.pinKeyId(), command.pinBlock(), card.getCardNumber());
    }

    // ========================================
    // ResetPINUseCase
    // ========================================

    /**
     * noRollbackFor: el intento fallido registrado ante un PIN actual incorrecto
     * debe persistir aunque la operación termine con BusinessRuleException.
     */
    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public Card execute(ResetPINCommand command) {
        log.info("Resetting PIN for card: {}", command.cardId());

        Card card = getCardOrThrow(command.cardId());
        ensureNotBlocked(card);

        // Validar PIN actual (si está configurado)
        if (card.isPINSet()) {
            boolean isOldPINValid = encryptionPort.verifyHash(
                    command.oldPlainPin(),
                    card.getPinHash()
            );

            if (!isOldPINValid) {
                boolean blocked = registerFailedAttempt(card);
                cardRepository.save(card);
                throw new BusinessRuleException(blocked
                        ? "El PIN actual es incorrecto. La tarjeta fue bloqueada por exceso de intentos"
                        : String.format("El PIN actual es incorrecto. Intentos restantes: %d",
                                card.getRemainingPinAttempts()));
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

    private boolean registerFailedAttempt(Card card) {
        CardStatus previousStatus = card.getStatus();
        boolean blocked = card.registerFailedPinAttempt();
        if (blocked) {
            eventEmitter.statusChanged(card, previousStatus, card.getBlockReason(), CardEventEmitter.SYSTEM_ACTOR);
            log.warn("Card {} blocked after {} failed PIN attempts", card.getId(), card.getPinFailedAttempts());
        } else {
            log.warn("Invalid PIN attempt for card: {} ({} remaining)", card.getId(), card.getRemainingPinAttempts());
        }
        return blocked;
    }

    private void ensureNotBlocked(Card card) {
        if (card.isBlocked()) {
            throw new BusinessRuleException(
                    String.format("La tarjeta está bloqueada. Razón: %s", card.getBlockReason())
            );
        }
    }

    private Card getCardOrThrow(CardId cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card", cardId.toString()));
    }
}
