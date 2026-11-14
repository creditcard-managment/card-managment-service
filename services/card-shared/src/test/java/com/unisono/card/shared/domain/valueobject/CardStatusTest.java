package com.unisono.card.shared.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.unisono.card.shared.domain.valueobject.CardStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class CardStatusTest {

    private static final Map<CardStatus, Set<CardStatus>> ALLOWED = Map.of(
            REQUESTED, EnumSet.of(APPROVED, REJECTED),
            APPROVED, EnumSet.of(ISSUED, CANCELLED),
            ISSUED, EnumSet.of(ACTIVE, BLOCKED, BLOCKED_FRAUD, EXPIRED, CANCELLED),
            ACTIVE, EnumSet.of(BLOCKED, BLOCKED_FRAUD, EXPIRED, CANCELLED),
            BLOCKED, EnumSet.of(ACTIVE, EXPIRED, CANCELLED),
            BLOCKED_FRAUD, EnumSet.of(ACTIVE, EXPIRED, CANCELLED),
            REJECTED, EnumSet.noneOf(CardStatus.class),
            CANCELLED, EnumSet.noneOf(CardStatus.class),
            EXPIRED, EnumSet.noneOf(CardStatus.class)
    );

    @ParameterizedTest
    @EnumSource(CardStatus.class)
    void shouldAllowOnlyDefinedTransitions(CardStatus from) {
        for (CardStatus to : CardStatus.values()) {
            assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(ALLOWED.get(from).contains(to));
        }
    }

    @ParameterizedTest
    @EnumSource(value = CardStatus.class, names = {"REJECTED", "CANCELLED", "EXPIRED"})
    void shouldNotLeaveFinalStates(CardStatus finalStatus) {
        assertThat(finalStatus.isFinal()).isTrue();
        assertThat(CardStatus.values())
                .noneMatch(finalStatus::canTransitionTo);
    }

    @ParameterizedTest
    @EnumSource(CardStatus.class)
    void shouldNotAllowSelfTransition(CardStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(CardStatus.class)
    void shouldBeConsistentWithCanBlock(CardStatus status) {
        assertThat(status.canTransitionTo(BLOCKED)).isEqualTo(status.canBlock());
        assertThat(status.canTransitionTo(BLOCKED_FRAUD)).isEqualTo(status.canBlock());
    }

    @Test
    void shouldRejectNullTarget() {
        assertThat(ACTIVE.canTransitionTo(null)).isFalse();
    }
}
