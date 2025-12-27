package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.valueobject.Money;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Categoría de la tarjeta (afecta límites y beneficios)
 */
@Getter
public enum CardCategory {
    CLASSIC("Clásica", Money.ofARS(50000)),
    GOLD("Gold", Money.ofARS(150000)),
    PLATINUM("Platinum", Money.ofARS(300000)),
    BLACK("Black", Money.ofARS(1000000)),
    INFINITE("Infinite", Money.ofARS(5000000));

    private final String displayName;
    private final Money defaultCreditLimit;

    CardCategory(String displayName, Money defaultCreditLimit) {
        this.displayName = displayName;
        this.defaultCreditLimit = defaultCreditLimit;
    }
}
