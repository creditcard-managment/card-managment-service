package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.ValidateCVVUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Servicio de validación de CVV.
 *
 * El CVV se guarda cifrado y el repositorio lo devuelve descifrado en el aggregate.
 * La comparación es de tiempo constante para no filtrar información por timing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CVVValidationService implements ValidateCVVUseCase {

    private final CardRepository cardRepository;

    @Override
    public boolean execute(ValidateCVVCommand command) {
        log.debug("Validating CVV for card: {}", command.cardId());

        Card card = cardRepository.findById(command.cardId())
                .orElseThrow(() -> new NotFoundException("Card", command.cardId().toString()));

        if (card.isBlocked()) {
            throw new BusinessRuleException(
                    String.format("La tarjeta está bloqueada. Razón: %s", card.getBlockReason())
            );
        }

        if (card.getCvv() == null) {
            log.warn("Card {} does not have a CVV assigned", command.cardId());
            return false;
        }

        boolean isValid = MessageDigest.isEqual(
                card.getCvv().getValue().getBytes(StandardCharsets.UTF_8),
                command.plainCvv().getBytes(StandardCharsets.UTF_8)
        );

        if (!isValid) {
            log.warn("Invalid CVV for card: {}", command.cardId());
        }
        return isValid;
    }
}
