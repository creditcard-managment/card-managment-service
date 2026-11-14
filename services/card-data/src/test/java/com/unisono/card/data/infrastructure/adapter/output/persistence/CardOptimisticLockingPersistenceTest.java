package com.unisono.card.data.infrastructure.adapter.output.persistence;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.service.CardExpirationService;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Control de concurrencia optimista de cards (@Version) contra PostgreSQL: una copia leída antes de un cambio
 * concurrente no puede pisarlo.
 */
class CardOptimisticLockingPersistenceTest extends CardDataPostgresTest {

    @Autowired
    private CardExpirationService expirationService;

    @Test
    void newCardStartsAtVersionZeroAndEachSaveIncrementsIt() {
        String id = save(CardStatus.ACTIVE, 12, 2030);
        assertThat(cards.findById(id).orElseThrow().getVersion()).isZero();

        Card card = load(id);
        card.block("Pérdida");
        Card saved = adapter.save(card);

        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(card.getVersion()).isEqualTo(1);
        assertThat(cards.findById(id).orElseThrow().getVersion()).isEqualTo(1);
    }

    @Test
    void staleCopyCannotOverwriteAConcurrentChange() {
        String id = save(CardStatus.ACTIVE, 12, 2030);
        Card first = load(id);
        Card second = load(id);

        first.block("Pérdida");
        adapter.save(first);

        second.cancel("Baja del cliente");
        assertThatThrownBy(() -> adapter.save(second)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(status(id)).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void sameInstanceCanBeSavedTwiceInOneTransaction() {
        String id = save(CardStatus.ACTIVE, 12, 2030);

        tx.executeWithoutResult(s -> {
            Card card = adapter.findById(CardId.of(id)).orElseThrow();
            card.block("Pérdida");
            adapter.save(card);
            card.unblock();
            adapter.save(card);
        });

        assertThat(status(id)).isEqualTo(CardStatus.ACTIVE);
        assertThat(cards.findById(id).orElseThrow().getVersion()).isEqualTo(2);
    }

    @Test
    void requestThatReadTheCardBeforeTheExpirationJobCannotUndoIt() {
        String id = save(CardStatus.ACTIVE, 11, 2026);
        Card readByRequest = load(id);

        expirationService.expireBatch(YearMonth.of(2026, 12), 100);

        readByRequest.block("Pérdida");
        assertThatThrownBy(() -> adapter.save(readByRequest))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(status(id)).isEqualTo(CardStatus.EXPIRED);
    }

    private Card load(String id) {
        return adapter.findById(CardId.of(id)).orElseThrow();
    }
}
