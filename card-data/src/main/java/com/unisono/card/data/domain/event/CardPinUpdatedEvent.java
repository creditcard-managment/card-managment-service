package com.unisono.card.data.domain.event;

import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando se actualiza el PIN de una tarjeta
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPinUpdatedEvent {

    private CardId cardId;
    private LocalDateTime updatedAt;
}
