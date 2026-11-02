package com.unisono.card.limits.infrastructure.adapter.output.cardvalidation;

import com.unisono.card.limits.domain.port.output.CardPort;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardValidationAdapter implements CardValidationPort {

    private final CardPort cardPort;

    @Override
    public boolean isCardActive(CardId cardId) {
        return cardPort.findById(cardId)
                .map(CardPort.CardInfo::isActive)
                .orElse(false);
    }

    @Override
    public boolean canAssignCreditLimit(CardId cardId) {
        return cardPort.findById(cardId)
                .map(CardPort.CardInfo::canAssignLimit)
                .orElse(false);
    }

    @Override
    public boolean cardExists(CardId cardId) {
        return cardPort.exists(cardId);
    }
}
