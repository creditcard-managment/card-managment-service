package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpirationDateTest {

    @Test
    void shouldReconstituteExpiredDateFromPreviousYear() {
        YearMonth past = YearMonth.now().minusYears(2);

        ExpirationDate date = ExpirationDate.of(past.getMonthValue(), past.getYear());

        assertThat(date.isExpired()).isTrue();
        assertThat(date.getYearMonth()).isEqualTo(past);
    }

    @Test
    void shouldParseExpiredDateFromString() {
        YearMonth past = YearMonth.now().minusYears(3);
        String value = String.format("%02d/%d", past.getMonthValue(), past.getYear());

        assertThat(ExpirationDate.of(value).isExpired()).isTrue();
        assertThat(ExpirationDate.fromString(value).isExpired()).isTrue();
    }

    @Test
    void shouldNotBeExpiredDuringCurrentMonth() {
        YearMonth now = YearMonth.now();

        assertThat(ExpirationDate.of(now.getMonthValue(), now.getYear()).isExpired()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1})
    void shouldRejectInvalidMonth(int month) {
        assertThatThrownBy(() -> ExpirationDate.of(month, 2030))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Mes");
    }

    @ParameterizedTest
    @ValueSource(ints = {1999, 2100, 30})
    void shouldRejectYearOutOfRange(int year) {
        assertThatThrownBy(() -> ExpirationDate.of(1, year))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Año");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1/2030", "13/2030", "12-2030", "12/30"})
    void shouldRejectMalformedString(String value) {
        assertThatThrownBy(() -> ExpirationDate.of(value))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldParseShortYearInFromString() {
        assertThat(ExpirationDate.fromString("03/30")).isEqualTo(ExpirationDate.of(3, 2030));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aa/30", "03", "03/30/1"})
    void shouldRejectMalformedFromString(String value) {
        assertThatThrownBy(() -> ExpirationDate.fromString(value))
                .isInstanceOf(InvalidValueObjectException.class);
    }

    @Test
    void shouldIssueWithinWindow() {
        YearMonth future = YearMonth.now().plusYears(5);

        ExpirationDate date = ExpirationDate.forIssuance(future.getMonthValue(), future.getYear());

        assertThat(date.getYearMonth()).isEqualTo(future);
    }

    @Test
    void shouldNotIssueExpiredDate() {
        YearMonth past = YearMonth.now().minusMonths(1);

        assertThatThrownBy(() -> ExpirationDate.forIssuance(past.getMonthValue(), past.getYear()))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("vencida");
    }

    @Test
    void shouldNotIssueMoreThanTenYearsAhead() {
        int year = YearMonth.now().getYear() + 11;

        assertThatThrownBy(() -> ExpirationDate.forIssuance(1, year))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("10");
    }

    @Test
    void shouldFormat() {
        ExpirationDate date = ExpirationDate.of(3, 2030);

        assertThat(date).hasToString("03/2030");
        assertThat(date.toShortString()).isEqualTo("03/30");
    }
}
