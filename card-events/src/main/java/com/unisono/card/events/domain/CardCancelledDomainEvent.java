package com.unisono.card.events.domain;

import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CardCancelledDomainEvent {
    private final CardId cardId;
    private final String maskedCardNumber;
    private final String reason;
    private final LocalDateTime cancelledAt;
}
