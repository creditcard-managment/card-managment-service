package com.unisono.card.data.domain.event;

import com.unisono.card.data.domain.model.Card;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Evento de dominio interno de Spring
 * Se publica cuando se crea una tarjeta
 */
@Getter
@RequiredArgsConstructor
public class CardCreatedDomainEvent {
    private final Card card;
}