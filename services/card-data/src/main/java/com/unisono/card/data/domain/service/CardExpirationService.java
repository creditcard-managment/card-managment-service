package com.unisono.card.data.domain.service;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.port.input.ExpireCardsUseCase;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Vence las tarjetas cuya fecha ya pasó (ISSUED, ACTIVE, BLOCKED o BLOCKED_FRAUD → EXPIRED).
 *
 * Cada lote es una transacción: las tarjetas quedan bloqueadas (FOR UPDATE SKIP LOCKED) hasta que se guardan
 * con su card.expired en el outbox. El autor del cambio es {@value CardEventEmitter#SYSTEM_ACTOR}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardExpirationService implements ExpireCardsUseCase {

    private final CardRepository cardRepository;
    private final CardEventEmitter eventEmitter;

    @Override
    @Transactional
    public List<CardId> expireBatch(YearMonth currentMonth, int limit) {
        List<CardId> expired = new ArrayList<>();
        for (Card card : cardRepository.lockExpiredBatch(currentMonth, limit)) {
            CardStatus previousStatus = card.getStatus();
            card.expire();
            Card saved = cardRepository.save(card);
            eventEmitter.statusChanged(saved, previousStatus, reasonFor(card), null);
            expired.add(saved.getId());
            log.info("⌛ Card {} expired ({} → EXPIRED, expiration date {})", saved.getId(), previousStatus,
                    card.getExpirationDate());
        }
        return expired;
    }

    private static String reasonFor(Card card) {
        return "Vencimiento por fecha (" + card.getExpirationDate().toShortString() + ")";
    }
}
