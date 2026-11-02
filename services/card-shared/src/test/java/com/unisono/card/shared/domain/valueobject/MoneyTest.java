package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldRoundToTwoDecimals() {
        assertThat(Money.ofARS(new BigDecimal("10.005")).getAmount()).isEqualByComparingTo("10.01");
    }

    @Test
    void shouldOperateOnSameCurrency() {
        Money total = Money.ofARS(100).add(Money.ofARS(50)).subtract(Money.ofARS(30));

        assertThat(total).isEqualTo(Money.ofARS(120));
        assertThat(total.isGreaterThan(Money.ofARS(100))).isTrue();
    }

    @Test
    void shouldRejectDifferentCurrenciesAsBusinessRule() {
        Money ars = Money.ofARS(100);
        Money usd = Money.of(new BigDecimal("100"), "USD");

        assertThatThrownBy(() -> ars.add(usd))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different currencies");
        assertThatThrownBy(() -> ars.isLessThan(usd))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> Money.of(null, "ARS"))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldRejectBlankCurrency() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, " "))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldRejectUnknownCurrency() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "XXZ"))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("XXZ");
    }

    @Test
    void shouldRejectDivisionByZero() {
        assertThatThrownBy(() -> Money.ofARS(100).divide(BigDecimal.ZERO))
                .isInstanceOf(InvalidValueObjectException.class);
    }
}
