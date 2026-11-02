package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import com.unisono.card.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectValidationTest {

    @Test
    void allValueObjectsShouldThrowInvalidValueObjectException() {
        List<Executable> invalid = List.of(
                () -> CardId.of(" "),
                () -> ClientId.of("12"),
                () -> CardNumber.of("4532123456789011"),
                () -> CVV.of("12"),
                () -> PIN.of("1234"),
                () -> Percentage.of(2.0),
                () -> Money.of(null, "ARS"),
                () -> ExpirationDate.of(13, 2030)
        );

        invalid.forEach(call -> assertThatThrownBy(call::execute)
                .isInstanceOf(InvalidValueObjectException.class));
    }

    @Test
    void invalidValueObjectShouldBeAValidationException() {
        assertThat(new InvalidValueObjectException("x")).isInstanceOf(ValidationException.class);
    }
}
