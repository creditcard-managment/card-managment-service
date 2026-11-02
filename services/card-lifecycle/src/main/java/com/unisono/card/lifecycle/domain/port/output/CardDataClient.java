package com.unisono.card.lifecycle.domain.port.output;

import com.unisono.card.shared.domain.valueobject.*;

import java.time.LocalDateTime;

public interface CardDataClient  {

    void updateStatus(CardId cardId, String status);

    void updateActivatedAt(CardId cardId, LocalDateTime activatedAt);

    void updateCardData(
            CardId cardId,
            CardNumber cardNumber,
            CVV cvv,
            PIN pin,
            ExpirationDate expirationDate
    );
}
