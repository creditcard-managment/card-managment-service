package com.unisono.card.events.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unisono.card.events.config.EventSerializationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CardStatusChangeTest {

    private static final String CARD_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 2, 10, 15, 30);

    private final ObjectMapper mapper = EventSerializationConfig.objectMapper();

    static Stream<Arguments> changes() {
        return Stream.of(
                Arguments.of(new CardApprovedEvent(CARD_ID, "REQUESTED", "ok", "admin"), "APPROVED", "admin", false),
                Arguments.of(new CardRejectedEvent(CARD_ID, "REQUESTED", "no", "admin"), "REJECTED", "admin", false),
                Arguments.of(new CardIssuedEvent(CARD_ID, "APPROVED", "0366", 12, 2030, "e", "system"), "ISSUED", "system", false),
                Arguments.of(new CardActivatedEvent(CARD_ID, "ISSUED", AT, "a", "cliente"), "ACTIVE", "cliente", true),
                Arguments.of(new CardBlockedEvent(CARD_ID, "ACTIVE", "NORMAL", AT, "b", "admin"), "BLOCKED", "admin", true),
                Arguments.of(new CardBlockedEvent(CARD_ID, "ACTIVE", "FRAUD", AT, "f", "antifraude"), "BLOCKED_FRAUD", "antifraude", true),
                Arguments.of(new CardUnblockedEvent(CARD_ID, "BLOCKED", AT, "u", "admin"), "ACTIVE", "admin", true),
                Arguments.of(new CardCancelledEvent(CARD_ID, "ACTIVE", AT, "c", "cliente"), "CANCELLED", "cliente", true),
                Arguments.of(new CardExpiredEvent(CARD_ID, "ACTIVE", AT, "v", "system"), "EXPIRED", "system", true)
        );
    }

    @ParameterizedTest
    @MethodSource("changes")
    void exposesTheTransition(CardStatusChange change, String newStatus, String actor, boolean hasOwnDate) {
        assertThat(change.getCardId()).isEqualTo(CARD_ID);
        assertThat(change.newStatus()).isEqualTo(newStatus);
        assertThat(change.actor()).isEqualTo(actor);
        assertThat(change.changedAt()).isEqualTo(hasOwnDate ? AT : ((DomainEvent) change).getOccurredOn());
    }

    @ParameterizedTest
    @MethodSource("changes")
    void derivedValuesAreNotPartOfTheJsonContract(CardStatusChange change) throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(change));

        assertThat(json.has("previousStatus")).isTrue();
        assertThat(json.has("newStatus")).isFalse();
        assertThat(json.has("actor")).isFalse();
        assertThat(json.has("changedAt")).isFalse();
    }

    @Test
    void messagePublishedBeforePreviousStatusExistedIsStillReadable() throws Exception {
        String old = """
                {"eventId":"e-1","occurredOn":"2026-11-02T10:15:30","eventType":"CardBlockedEvent",
                 "cardId":"%s","blockType":"NORMAL","blockedAt":"2026-11-02T10:15:30","reason":"b","blockedBy":"admin"}
                """.formatted(CARD_ID);

        CardBlockedEvent event = mapper.readValue(old, CardBlockedEvent.class);

        assertThat(event.getPreviousStatus()).isNull();
        assertThat(event.newStatus()).isEqualTo("BLOCKED");
    }
}
