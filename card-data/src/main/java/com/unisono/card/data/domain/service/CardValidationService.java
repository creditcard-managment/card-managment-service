package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardStatus;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de validación de reglas de negocio
 */
@Slf4j
@Service
public class CardValidationService {

    /**
     * Valida que la tarjeta pueda realizar operaciones
     */
    public void validateCardIsOperational(Card card) {
        if (!card.isOperational()) {
            if (card.isBlocked()) {
                throw new BusinessRuleException(
                        String.format("La tarjeta está bloqueada. Razón: %s", card.getBlockReason())
                );
            }
            if (card.getExpirationDate().isExpired()) {
                throw new BusinessRuleException("La tarjeta está vencida");
            }
            throw new BusinessRuleException(
                    String.format("La tarjeta no está operativa. Estado: %s", card.getStatus())
            );
        }
    }

    /**
     * Valida que la tarjeta esté en un estado específico
     */
    public void validateCardStatus(Card card, CardStatus expectedStatus) {
        if (card.getStatus() != expectedStatus) {
            throw new BusinessRuleException(
                    String.format("La tarjeta debe estar en estado %s. Estado actual: %s",
                            expectedStatus, card.getStatus())
            );
        }
    }

    /**
     * Valida que la tarjeta no requiera renovación
     */
    public void validateCardNotExpiring(Card card) {
        if (card.requiresRenewal()) {
            log.warn("Card {} is near expiration and requires renewal", card.getId());
            throw new BusinessRuleException("La tarjeta está próxima a vencer y requiere renovación");
        }
    }
}
