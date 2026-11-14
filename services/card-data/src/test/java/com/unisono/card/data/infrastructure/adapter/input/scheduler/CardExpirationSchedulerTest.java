package com.unisono.card.data.infrastructure.adapter.input.scheduler;

import com.unisono.card.data.domain.port.input.ExpireCardsUseCase;
import com.unisono.card.shared.domain.valueobject.CardId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    private ExpireCardsUseCase expireCards;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void processesBatchesUntilOneIsNotFull() {
        when(expireCards.expireBatch(any(), eq(2))).thenReturn(ids(2), ids(2), ids(1));

        int total = scheduler("2026-12-01T03:05:00Z", 2).run();

        assertThat(total).isEqualTo(5);
        verify(expireCards, times(3)).expireBatch(YearMonth.of(2026, 12), 2);
        assertThat(meterRegistry.get("card.data.cards.expired").counter().count()).isEqualTo(5);
    }

    @Test
    void usesTheMonthOfTheConfiguredZone() {
        when(expireCards.expireBatch(any(), anyInt())).thenReturn(List.of());

        // 1/12 00:30 en UTC todavía es 30/11 21:30 en Argentina: las tarjetas 11/26 no vencieron
        scheduler("2026-12-01T00:30:00Z", 100).run();

        verify(expireCards).expireBatch(YearMonth.of(2026, 11), 100);
    }

    @Test
    void stopsWhenABatchFails() {
        when(expireCards.expireBatch(any(), eq(2)))
                .thenReturn(ids(2))
                .thenThrow(new IllegalStateException("db down"));

        int total = scheduler("2026-12-01T03:05:00Z", 2).run();

        assertThat(total).isEqualTo(2);
        verify(expireCards, times(2)).expireBatch(any(), eq(2));
    }

    private CardExpirationScheduler scheduler(String instant, int batchSize) {
        return new CardExpirationScheduler(expireCards, meterRegistry,
                Clock.fixed(Instant.parse(instant), ARGENTINA), batchSize);
    }

    private static List<CardId> ids(int count) {
        return Collections.nCopies(count, CardId.of(UUID.randomUUID().toString()));
    }
}
