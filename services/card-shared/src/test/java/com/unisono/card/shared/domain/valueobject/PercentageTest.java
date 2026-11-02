package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PercentageTest {

    @Test
    void shouldCreateFromPercentage() {
        Percentage percentage = Percentage.fromPercentage(new BigDecimal("15"));

        assertThat(percentage.getValue()).isEqualByComparingTo("0.15");
        assertThat(percentage.asPercentage()).isEqualByComparingTo("15");
    }

    @Test
    void shouldAcceptPercentageBounds() {
        assertThat(Percentage.fromPercentage(BigDecimal.ZERO).getValue()).isEqualByComparingTo("0");
        assertThat(Percentage.fromPercentage(new BigDecimal("100")).getValue()).isEqualByComparingTo("1");
    }

    @Test
    void shouldRejectPercentageAboveHundred() {
        assertThatThrownBy(() -> Percentage.fromPercentage(new BigDecimal("150")))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("100%");
    }

    @Test
    void shouldRejectNegativePercentage() {
        assertThatThrownBy(() -> Percentage.fromPercentage(new BigDecimal("-1")))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldCreateFromFraction() {
        assertThat(Percentage.of(0.3).asPercentage()).isEqualByComparingTo("30");
    }

    @Test
    void shouldRejectFractionAboveOne() {
        assertThatThrownBy(() -> Percentage.of(new BigDecimal("15")))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldApplyToMoney() {
        Money result = Percentage.fromPercentage(new BigDecimal("30")).applyTo(Money.ofARS(1000));

        assertThat(result).isEqualTo(Money.ofARS(300));
    }
}
