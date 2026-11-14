package com.unisono.card.data.domain.port.input;

import com.unisono.card.shared.domain.valueobject.CardId;

import java.time.YearMonth;
import java.util.List;

/**
 * Port de entrada: vencimiento de tarjetas por fecha
 */
public interface ExpireCardsUseCase {

    /**
     * Vence, en una sola transacción, hasta {@code limit} tarjetas cuya fecha de vencimiento es anterior a
     * {@code currentMonth}, y encola un card.expired por cada una.
     *
     * @return las tarjetas vencidas; si son menos que {@code limit}, no quedan más pendientes
     */
    List<CardId> expireBatch(YearMonth currentMonth, int limit);
}
