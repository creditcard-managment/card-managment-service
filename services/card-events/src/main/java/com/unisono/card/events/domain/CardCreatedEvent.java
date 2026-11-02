package com.unisono.card.events.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se crea una nueva tarjeta
 */
@Getter
@ToString(callSuper = true)
public class CardCreatedEvent extends DomainEvent {

    private final String cardId;
    private final String clientId;
    private final String holderFullName;
    private final String cardType;
    private final String cardCategory;
    private final String createdBy;

    @JsonCreator
    public CardCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("cardId") String cardId,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("holderFullName") String holderFullName,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardCategory") String cardCategory,
            @JsonProperty("createdBy") String createdBy
    ) {
        super(eventId, occurredOn);
        this.cardId = cardId;
        this.clientId = clientId;
        this.holderFullName = holderFullName;
        this.cardType = cardType;
        this.cardCategory = cardCategory;
        this.createdBy = createdBy;
    }

    public CardCreatedEvent(
            String cardId,
            String clientId,
            String holderFullName,
            String cardType,
            String cardCategory,
            String createdBy
    ) {
        super();
        this.cardId = cardId;
        this.clientId = clientId;
        this.holderFullName = holderFullName;
        this.cardType = cardType;
        this.cardCategory = cardCategory;
        this.createdBy = createdBy;
    }
}
