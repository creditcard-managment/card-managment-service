package com.unisono.card.limits.infrastructure.adapter.output.card;

import com.unisono.card.limits.domain.port.output.CardPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardAdapter implements CardPort {

    private final CardJpaRepository repository;

    @Override
    public Optional<CardInfo> findById(CardId cardId) {
        return repository.findById(cardId.getValue())
                .map(this::toCardInfo);
    }

    @Override
    public boolean exists(CardId cardId) {
        return repository.existsById(cardId.getValue());
    }

    private CardInfo toCardInfo(CardJpaEntity entity) {
        return new CardInfo(
                CardId.of(entity.getId()), // ✅ Factory method
                CardStatus.valueOf(entity.getStatus()),
                entity.getDeleted()
        );
    }
}
