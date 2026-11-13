package com.unisono.card.payment.infrastructure.adapter.output.persistence;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.unisono.card.payment.support.TestTransactions.CARD_ID;
import static com.unisono.card.payment.support.TestTransactions.approvedPurchase;
import static com.unisono.card.payment.support.TestTransactions.approvedRefund;
import static com.unisono.card.payment.support.TestTransactions.authorizedPurchase;
import static com.unisono.card.payment.support.TestTransactions.cardData;
import static com.unisono.card.payment.support.TestTransactions.declinedPurchase;
import static com.unisono.card.payment.support.TestTransactions.merchant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TransactionPersistenceAdapter contra PostgreSQL: mapeo completo, versionado, consultas y lock de fila
 */
class TransactionPersistenceTest extends PostgresPersistenceTest {

    @Autowired
    TransactionRepository repository;

    @Test
    void shouldStoreAndLoadATransactionWithItsWholeState() {
        Transaction purchase = Transaction.createPurchase(CARD_ID, cardData(), merchant(),
                TransactionAmount.withConversion(Money.of(new BigDecimal("100.00"), "USD"),
                        Money.ofARS(new BigDecimal("98000.00")), new BigDecimal("980.00")),
                PaymentMethod.CHIP);
        purchase.authorize(AuthorizationCode.approved("A1B2C3"), 15);
        purchase.approve();
        purchase.setTerminalInfo("TERM-01", "051");

        save(purchase);
        Transaction loaded = tx.execute(s -> repository.findById(purchase.getTransactionId()).orElseThrow());

        assertThat(loaded.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(loaded.getAmount().getLocalAmount()).isEqualTo(Money.ofARS(new BigDecimal("98000.00")));
        assertThat(loaded.getAuthorizationCode().getCode()).isEqualTo("A1B2C3");
        assertThat(loaded.getRiskScore()).isEqualTo(15);
        assertThat(loaded.getTerminalId()).isEqualTo("TERM-01");
        assertThat(loaded.getTransactionDate()).isEqualToIgnoringNanos(purchase.getTransactionDate());
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    void shouldIncrementTheVersionAndRejectStaleUpdates() {
        Transaction purchase = save(approvedPurchase(1_000));
        Transaction first = tx.execute(s -> repository.findById(purchase.getTransactionId()).orElseThrow());
        Transaction stale = tx.execute(s -> repository.findById(purchase.getTransactionId()).orElseThrow());

        first.settle();
        Transaction settled = save(first);
        stale.reverse("Llegó tarde");

        assertThat(settled.getVersion()).isEqualTo(1);
        assertThatThrownBy(() -> save(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void shouldFindTheRefundsOfATransactionInOrder() {
        Transaction original = save(approvedPurchase(10_000));
        Transaction first = save(approvedRefund(1_000, original.getTransactionId()));
        Transaction second = save(approvedRefund(2_000, original.getTransactionId()));
        save(approvedRefund(500, TransactionId.generate()));

        List<Transaction> refunds = tx.execute(s -> repository.findRefundsOf(original.getTransactionId()));

        assertThat(refunds).extracting(Transaction::getTransactionId)
                .containsExactly(first.getTransactionId(), second.getTransactionId());
    }

    @Test
    void shouldFindOnlyApprovedTransactionsPendingSettlement() {
        Transaction pending = save(approvedPurchase(1_000));
        Transaction settled = approvedPurchase(2_000);
        settled.settle();
        save(settled);
        save(declinedPurchase(3_000, "51"));
        save(authorizedPurchase(4_000));

        assertThat(inTx(() -> repository.findPendingSettlement()))
                .extracting(Transaction::getTransactionId)
                .containsExactly(pending.getTransactionId());
    }

    @Test
    void shouldFindPreAuthorizationsAuthorizedBeforeTheThreshold() {
        Transaction preAuthorization = save(authorizedPurchase(1_000));
        save(approvedPurchase(2_000));

        assertThat(inTx(() -> repository.findPreAuthorizationsAuthorizedBefore(LocalDateTime.now().plusMinutes(1))))
                .extracting(Transaction::getTransactionId)
                .containsExactly(preAuthorization.getTransactionId());
        assertThat(inTx(() -> repository.findPreAuthorizationsAuthorizedBefore(LocalDateTime.now().minusHours(1))))
                .isEmpty();
    }

    @Test
    void shouldFilterByCardStatusAndDateRange() {
        Transaction approved = save(approvedPurchase(1_000));
        Transaction declined = save(declinedPurchase(2_000, "51"));
        CardId otherCard = CardId.of("660e8400-e29b-41d4-a716-446655440000");

        assertThat(inTx(() -> repository.findByCardId(CARD_ID))).hasSize(2);
        assertThat(inTx(() -> repository.findByCardId(otherCard))).isEmpty();
        assertThat(inTx(() -> repository.findByCardIdAndStatus(CARD_ID, TransactionStatus.DECLINED)))
                .extracting(Transaction::getTransactionId).containsExactly(declined.getTransactionId());
        assertThat(inTx(() -> repository.findByCardIdAndDateRange(CARD_ID,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5))))
                .extracting(Transaction::getTransactionId)
                .containsExactlyInAnyOrder(approved.getTransactionId(), declined.getTransactionId());
        assertThat(inTx(() -> repository.findByCardIdAndDateRange(CARD_ID,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1)))).isEmpty();
    }

    @Test
    void shouldMakeASecondLockerWaitUntilTheFirstTransactionEnds() throws Exception {
        Transaction purchase = save(approvedPurchase(1_000));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> tx.executeWithoutResult(s -> {
            repository.findByIdForUpdate(purchase.getTransactionId()).orElseThrow();
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Long> second = CompletableFuture.supplyAsync(() -> tx.execute(s -> {
            repository.findByIdForUpdate(purchase.getTransactionId()).orElseThrow();
            return System.nanoTime();
        }));

        Thread.sleep(500);
        assertThat(second).isNotDone();

        long releasedAt = System.nanoTime();
        release.countDown();
        first.get(10, TimeUnit.SECONDS);
        assertThat(second.get(10, TimeUnit.SECONDS)).isGreaterThan(releasedAt);
    }

    private List<Transaction> inTx(java.util.function.Supplier<List<Transaction>> query) {
        return tx.execute(s -> query.get());
    }

    private Transaction save(Transaction transaction) {
        return tx.execute(s -> repository.save(transaction));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
