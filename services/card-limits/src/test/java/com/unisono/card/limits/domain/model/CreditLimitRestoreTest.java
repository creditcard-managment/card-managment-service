package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * restoreConsumption: reversos y devoluciones (a diferencia de releaseCredit, que modela el pago del resumen)
 */
class CreditLimitRestoreTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    private CreditLimit creditLimit;

    @BeforeEach
    void setUp() {
        creditLimit = CreditLimit.assign(CARD_ID, Money.ofARS(500_000), 0.3, "test");
        creditLimit.consumeForPurchase(Money.ofARS(20_000));
    }

    @Test
    void shouldRestoreAvailableDailyAndMonthlyForTodaysTransaction() {
        Money notRestored = creditLimit.restoreConsumption(Money.ofARS(20_000), LocalDate.now());

        assertThat(notRestored.isZero()).isTrue();
        assertThat(creditLimit.getConsumedCredit().getAmount().isZero()).isTrue();
        assertThat(creditLimit.getAvailableCredit().getAmount()).isEqualTo(Money.ofARS(500_000));
        assertThat(creditLimit.getDailyLimit().getConsumed().isZero()).isTrue();
        assertThat(creditLimit.getMonthlyLimit().getConsumed().isZero()).isTrue();
    }

    @Test
    void shouldRestorePartialAmount() {
        creditLimit.restoreConsumption(Money.ofARS(5_000), LocalDate.now());

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(15_000));
        assertThat(creditLimit.getDailyLimit().getConsumed()).isEqualTo(Money.ofARS(15_000));
        assertThat(creditLimit.getMonthlyLimit().getConsumed()).isEqualTo(Money.ofARS(15_000));
    }

    @Test
    void shouldNotTouchDailyQuotaOfAnotherDay() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        creditLimit.restoreConsumption(Money.ofARS(5_000), yesterday);

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(15_000));
        assertThat(creditLimit.getDailyLimit().getConsumed()).isEqualTo(Money.ofARS(20_000));
        boolean sameMonth = yesterday.getMonth() == LocalDate.now().getMonth();
        assertThat(creditLimit.getMonthlyLimit().getConsumed())
                .isEqualTo(sameMonth ? Money.ofARS(15_000) : Money.ofARS(20_000));
    }

    @Test
    void shouldNotTouchMonthlyQuotaOfAnotherMonth() {
        creditLimit.restoreConsumption(Money.ofARS(5_000), LocalDate.now().minusMonths(1));

        assertThat(creditLimit.getMonthlyLimit().getConsumed()).isEqualTo(Money.ofARS(20_000));
        assertThat(creditLimit.getDailyLimit().getConsumed()).isEqualTo(Money.ofARS(20_000));
    }

    @Test
    void shouldCapConsumedAtZeroAndReportTheExcessWhenAlreadyPaid() {
        creditLimit.releaseCredit(Money.ofARS(15_000)); // pago del resumen: consumido = 5.000

        Money notRestored = creditLimit.restoreConsumption(Money.ofARS(20_000), LocalDate.now());

        assertThat(notRestored).isEqualTo(Money.ofARS(15_000));
        assertThat(creditLimit.getConsumedCredit().getAmount().isZero()).isTrue();
        assertThat(creditLimit.getAvailableCredit().getAmount()).isEqualTo(Money.ofARS(500_000));
        assertThat(creditLimit.getDailyLimit().getConsumed().isZero()).isTrue();
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() -> creditLimit.restoreConsumption(Money.ofARS(0), LocalDate.now()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRevertAnOperationOfAnotherDayOnlyInTheConsumedCreditAndMonth() {
        CreditLimit.ConsumptionSnapshot before = creditLimit.consumptionSnapshot();
        creditLimit.consumeForPurchase(Money.ofARS(10_000));
        CreditOperation applied = CreditOperation.applied("op", creditLimit.getId(), CreditOperation.Type.CONSUME_PURCHASE,
                before, creditLimit.consumptionSnapshot(), "compra");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CreditOperation fromYesterday = CreditOperation.reconstitute("op", creditLimit.getId(),
                CreditOperation.Type.CONSUME_PURCHASE, CreditOperation.Status.APPLIED, applied.getCreditDelta(),
                applied.getDailyDelta(), applied.getMonthlyDelta(), yesterday, "compra", null, null);

        creditLimit.revertOperation(fromYesterday);

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(20_000));
        assertThat(creditLimit.getDailyLimit().getConsumed()).isEqualTo(Money.ofARS(30_000));
        boolean sameMonth = yesterday.getMonth() == LocalDate.now().getMonth();
        assertThat(creditLimit.getMonthlyLimit().getConsumed())
                .isEqualTo(sameMonth ? Money.ofARS(20_000) : Money.ofARS(30_000));
    }
}
