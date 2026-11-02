package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardNumberTest {

    @ParameterizedTest
    @CsvSource({
            "4222222222222,       * **** **** 2222",
            "378282246310005,     *** **** **** 0005",
            "4532015112830366,    **** **** **** 0366",
            "6011000000000000001, *** **** **** **** 0001"
    })
    void shouldMaskAllButLastFourDigits(String pan, String expected) {
        CardNumber cardNumber = CardNumber.of(pan);

        assertThat(cardNumber.getMasked()).isEqualTo(expected);
        assertThat(cardNumber.getMasked().replace(" ", "")).hasSize(pan.length());
        assertThat(cardNumber).hasToString(expected);
    }

    @Test
    void shouldSanitizeSpacesAndDashes() {
        CardNumber cardNumber = CardNumber.of("4532 0151-1283 0366");

        assertThat(cardNumber.getValue()).isEqualTo("4532015112830366");
        assertThat(cardNumber.getLastFourDigits()).isEqualTo("0366");
        assertThat(cardNumber.getBIN()).isEqualTo("453201");
    }

    @ParameterizedTest
    @ValueSource(strings = {"4532015112830367", "453201511283", "45320151128303660000", "4532a15112830366"})
    void shouldRejectInvalidNumbers(String pan) {
        assertThatThrownBy(() -> CardNumber.of(pan))
                .isInstanceOf(InvalidValueObjectException.class);
    }
}
