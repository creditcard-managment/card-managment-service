package com.unisono.card.data.domain.event;

import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de dominio que se dispara cuando se actualizan
 * los datos de contacto del titular de una tarjeta
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardHolderDataUpdatedEvent {

    private CardId cardId;
    private String holderEmail;
    private String holderPhoneNumber;
    private String holderAddress;
    private LocalDateTime updatedAt;
}
