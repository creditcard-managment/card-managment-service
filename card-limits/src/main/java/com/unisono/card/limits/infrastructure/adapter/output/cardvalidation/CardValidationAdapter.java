package com.unisono.card.limits.infrastructure.adapter.output.cardvalidation;

import com.unisono.card.data.domain.model.CardStatus;
import com.unisono.card.data.domain.port.input.GetCardUseCase;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador para validar tarjetas
 *
 * Implementa CardValidationPort usando GetCardUseCase de card-data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardValidationAdapter implements CardValidationPort {

    private final GetCardUseCase getCardUseCase;

    @Override
    public boolean cardExists(CardId cardId) {
        log.debug("Checking if card exists: {}", cardId);
        return getCardUseCase.findById(cardId).isPresent();
    }

    @Override
    public boolean isCardActive(CardId cardId) {
        log.debug("Checking if card is active: {}", cardId);

        return getCardUseCase.findById(cardId)
                .map(card -> card.getStatus() == CardStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    public boolean canAssignCreditLimit(CardId cardId) {
        log.debug("Checking if card can receive credit limit: {}", cardId);

        return getCardUseCase.findById(cardId)
                .map(card -> {
                    // Solo se puede asignar límite a tarjetas APPROVED, ISSUED o ACTIVE
                    return card.getStatus() == CardStatus.APPROVED
                            || card.getStatus() == CardStatus.ISSUED
                            || card.getStatus() == CardStatus.ACTIVE;
                })
                .orElse(false);
    }
}
