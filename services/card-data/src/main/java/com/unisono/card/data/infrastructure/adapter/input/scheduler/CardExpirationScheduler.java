package com.unisono.card.data.infrastructure.adapter.input.scheduler;

import com.unisono.card.data.domain.port.input.ExpireCardsUseCase;
import com.unisono.card.shared.domain.valueobject.CardId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * Job diario de vencimiento de tarjetas
 *
 * Corre con {@code card.data.expiration.cron} (por defecto 00:05) en la zona {@code card.data.expiration.zone}
 * (por defecto America/Argentina/Buenos_Aires), que también define en qué mes estamos: una tarjeta 11/26
 * vence en la primera ejecución del 1 de diciembre. Procesa lotes de {@code batch-size} hasta que no quedan
 * pendientes; si un día no corre, la ejecución siguiente vence las atrasadas. Con cron "-" queda desactivado.
 */
@Slf4j
@Component
public class CardExpirationScheduler {

    private final ExpireCardsUseCase expireCards;
    private final Clock clock;
    private final int batchSize;
    private final Counter expiredCounter;

    public CardExpirationScheduler(
            ExpireCardsUseCase expireCards,
            MeterRegistry meterRegistry,
            @Value("${card.data.expiration.zone:America/Argentina/Buenos_Aires}") String zone,
            @Value("${card.data.expiration.batch-size:100}") int batchSize) {
        this(expireCards, meterRegistry, Clock.system(ZoneId.of(zone)), batchSize);
    }

    CardExpirationScheduler(ExpireCardsUseCase expireCards, MeterRegistry meterRegistry, Clock clock, int batchSize) {
        this.expireCards = expireCards;
        this.clock = clock;
        this.batchSize = batchSize;
        this.expiredCounter = Counter.builder("card.data.cards.expired")
                .description("Cards expired by the daily expiration job")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${card.data.expiration.cron:0 5 0 * * *}",
            zone = "${card.data.expiration.zone:America/Argentina/Buenos_Aires}")
    public void expireDueCards() {
        run();
    }

    /**
     * @return la cantidad de tarjetas vencidas en esta ejecución
     */
    int run() {
        YearMonth currentMonth = YearMonth.now(clock);
        int total = 0;
        while (true) {
            List<CardId> expired;
            try {
                expired = expireCards.expireBatch(currentMonth, batchSize);
            } catch (RuntimeException e) {
                // El lote se revirtió entero; las tarjetas quedan pendientes para la próxima ejecución
                log.error("⌛ Card expiration stopped after {} card(s): batch failed", total, e);
                break;
            }
            total += expired.size();
            expiredCounter.increment(expired.size());
            if (expired.size() < batchSize) {
                break;
            }
        }
        log.info("⌛ Card expiration for {}: {} card(s) expired", currentMonth, total);
        return total;
    }
}
