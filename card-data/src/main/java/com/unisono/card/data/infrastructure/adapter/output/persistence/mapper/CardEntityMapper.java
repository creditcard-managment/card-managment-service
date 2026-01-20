package com.unisono.card.data.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.CardEntity;
import com.unisono.card.shared.domain.valueobject.CVV;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.domain.valueobject.ClientId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper entre Card (domain) y CardEntity (persistence)
 */
@RequiredArgsConstructor
@Component
public class CardEntityMapper {

    /**
     * Convierte de CardEntity (persistencia) a Card (dominio)
     */
    public Card toDomain(CardEntity entity) {
        Card card = new Card();

        // ✅ Convertir String → CardId
        card.setId(new CardId(entity.getCardId()));

        card.setStatus(entity.getStatus());

        // ✅ Convertir String → ClientId
        card.setClientId(new ClientId(entity.getClientId()));

        // Datos de emisión
        if (entity.getCardNumber() != null) {
            card.setCardNumber(new CardNumber(entity.getCardNumber()));
        }

        if (entity.getCvv() != null) {
            card.setCvv(new CVV(entity.getCvv()));
        }

        card.setPinHash(entity.getPinHash());

        if (entity.getExpirationMonth() != null && entity.getExpirationYear() != null) {
            card.setExpirationDate(new ExpirationDate(
                    entity.getExpirationMonth(),
                    entity.getExpirationYear()
            ));
        }

        // ✅ CardHolder (construir desde los campos de la entidad)
        if (entity.getHolderFirstName() != null && entity.getHolderLastName() != null) {
            CardHolder holder = new CardHolder(
                    entity.getHolderFirstName(),
                    entity.getHolderLastName(),
                    entity.getHolderDocumentNumber(),
                    entity.getHolderEmail(),
                    entity.getHolderPhoneNumber(),
                    entity.getHolderBirthDate(),
                    entity.getHolderAddress()
            );
            card.setHolder(holder);
        }

        return card;
    }

    /**
     * Convierte de Card (dominio) a CardEntity (persistencia)
     */
    public CardEntity toEntity(Card card) {
        CardEntity entity = new CardEntity();

        // ✅ Convertir CardId → String
        entity.setCardId(card.getId().getValue());

        entity.setStatus(card.getStatus());

        // ✅ Si clientId es ClientId (Value Object):
        // entity.setClientId(card.getClientId().value());
        // Si es String:
        entity.setClientId(card.getClientId().toString());

        // Datos de emisión
        if (card.getCardNumber() != null) {
            entity.setCardNumber(card.getCardNumber().getValue());
        }

        if (card.getCvv() != null) {
            entity.setCvv(card.getCvv().getValue());
        }

        entity.setPinHash(card.getPinHash());

        if (card.getExpirationDate() != null) {
            entity.setExpirationMonth(card.getExpirationDate().getMonth());
            entity.setExpirationYear(card.getExpirationDate().getYear());
        }

        // ✅ CardHolder (descomponer en campos separados)
        if (card.getHolder() != null) {
            entity.setHolderFirstName(card.getHolder().getFirstName());
            entity.setHolderLastName(card.getHolder().getLastName());
            entity.setHolderDocumentNumber(card.getHolder().getDocumentNumber());
            entity.setHolderEmail(card.getHolder().getEmail());
            entity.setHolderPhoneNumber(card.getHolder().getPhoneNumber());
            entity.setHolderBirthDate(card.getHolder().getBirthDate());
            entity.setHolderAddress(card.getHolder().getAddress());
        }

        return entity;
    }
}
