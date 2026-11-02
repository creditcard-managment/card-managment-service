package com.unisono.card.shared.domain.valueobject;

import com.unisono.card.shared.domain.exception.InvalidValueObjectException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardIdTest {

    @Test
    void shouldAcceptCanonicalUuid() {
        assertThat(CardId.of("550e8400-e29b-41d4-a716-446655440000").getValue())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void shouldNormalizeToLowerCase() {
        assertThat(CardId.of("550E8400-E29B-41D4-A716-446655440000"))
                .isEqualTo(CardId.of("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void shouldGenerateValidId() {
        CardId generated = CardId.generate();

        assertThat(CardId.of(generated.getValue())).isEqualTo(generated);
    }

    @ParameterizedTest
    @ValueSource(strings = {"card-123", "1-1-1-1-1", "550e8400e29b41d4a716446655440000",
            "550e8400-e29b-41d4-a716-44665544000z", " 550e8400-e29b-41d4-a716-446655440000"})
    void shouldRejectNonUuid(String value) {
        assertThatThrownBy(() -> CardId.of(value))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("UUID");
    }
}
