package com.unisono.card.limits.domain.service;

import com.unisono.card.limits.domain.exception.CreditOperationCancelledException;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.model.CreditOperation;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.CancelOperationCommand;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.CancelOperationResult;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.CancelOutcome;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.ConsumeCreditCommand;
import com.unisono.card.limits.domain.port.input.RecalculateAvailableCreditUseCase.RestoreCreditCommand;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.limits.domain.port.output.CreditOperationRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Operaciones de crédito con operationId: idempotencia, cancelación exacta y anulación antes de llegar
 */
class CreditOperationIdempotencyTest {

    private static final CardId CARD_ID = CardId.of("550e8400-e29b-41d4-a716-446655440000");

    private final InMemoryCreditLimits creditLimits = new InMemoryCreditLimits();
    private final InMemoryOperations operations = new InMemoryOperations();
    private CreditLimitManagementService service;
    private CreditLimit creditLimit;

    @BeforeEach
    void setUp() {
        service = new CreditLimitManagementService(creditLimits, mock(CardValidationPort.class), operations);
        creditLimit = creditLimits.save(CreditLimit.assign(CARD_ID, Money.ofARS(500_000), 0.3, "test"));
    }

    @Test
    void shouldApplyAConsumptionOnlyOnceForTheSameOperationId() {
        service.consumeForPurchase(consume("op-1", 20_000));
        service.consumeForPurchase(consume("op-1", 20_000));

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(20_000));
        CreditOperation operation = operations.findById("op-1").orElseThrow();
        assertThat(operation.getStatus()).isEqualTo(CreditOperation.Status.APPLIED);
        assertThat(operation.getType()).isEqualTo(CreditOperation.Type.CONSUME_PURCHASE);
        assertThat(operation.getCreditDelta()).isEqualTo(Money.ofARS(20_000));
        assertThat(operation.getDailyDelta()).isEqualTo(Money.ofARS(20_000));
        assertThat(operation.getMonthlyDelta()).isEqualTo(Money.ofARS(20_000));
    }

    @Test
    void shouldNotRecordOperationsWithoutId() {
        service.consumeForPurchase(new ConsumeCreditCommand(creditLimit.getId(), Money.ofARS(20_000), "compra"));

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(20_000));
        assertThat(operations.store).isEmpty();
    }

    @Test
    void shouldUndoAConsumptionExactlyAndOnlyOnce() {
        service.consumeForPurchase(consume("op-1", 20_000));

        CancelOperationResult first = service.cancelOperation(new CancelOperationCommand(creditLimit.getId(), "op-1"));
        CancelOperationResult second = service.cancelOperation(new CancelOperationCommand(creditLimit.getId(), "op-1"));

        assertThat(first.outcome()).isEqualTo(CancelOutcome.CANCELLED);
        assertThat(second.outcome()).isEqualTo(CancelOutcome.ALREADY_CANCELLED);
        assertThat(creditLimit.getConsumedCredit().getAmount().isZero()).isTrue();
        assertThat(creditLimit.getAvailableCredit().getAmount()).isEqualTo(Money.ofARS(500_000));
        assertThat(creditLimit.getDailyLimit().getConsumed().isZero()).isTrue();
        assertThat(creditLimit.getMonthlyLimit().getConsumed().isZero()).isTrue();
        assertThat(operations.findById("op-1").orElseThrow().getStatus()).isEqualTo(CreditOperation.Status.CANCELLED);
    }

    @Test
    void shouldVoidAnOperationThatHasNotArrivedAndRejectItLater() {
        CancelOperationResult result = service.cancelOperation(new CancelOperationCommand(creditLimit.getId(), "op-late"));

        assertThat(result.outcome()).isEqualTo(CancelOutcome.VOIDED);
        assertThatThrownBy(() -> service.consumeForPurchase(consume("op-late", 20_000)))
                .isInstanceOf(CreditOperationCancelledException.class);
        assertThat(creditLimit.getConsumedCredit().getAmount().isZero()).isTrue();
    }

    @Test
    void shouldUndoARestorationByConsumingBackExactlyWhatWasRestored() {
        service.consumeForPurchase(consume("op-1", 20_000));
        creditLimit.releaseCredit(Money.ofARS(15_000)); // pago del resumen: consumido = 5.000

        // Restaura 20.000 pero lo consumido solo baja 5.000 (no hay saldo a favor); el cupo diario baja 20.000
        service.restoreCredit(new RestoreCreditCommand(creditLimit.getId(), Money.ofARS(20_000), LocalDate.now(),
                "reverso", "op-2"));
        assertThat(creditLimit.getConsumedCredit().getAmount().isZero()).isTrue();

        service.cancelOperation(new CancelOperationCommand(creditLimit.getId(), "op-2"));

        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(5_000));
        assertThat(creditLimit.getDailyLimit().getConsumed()).isEqualTo(Money.ofARS(20_000));
        assertThat(creditLimit.getMonthlyLimit().getConsumed()).isEqualTo(Money.ofARS(20_000));
    }

    @Test
    void shouldRejectReplayingAnOperationIdWithAnotherType() {
        service.consumeForPurchase(consume("op-1", 20_000));

        assertThatThrownBy(() -> service.restoreCredit(new RestoreCreditCommand(creditLimit.getId(),
                Money.ofARS(20_000), LocalDate.now(), "reverso", "op-1")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(20_000));
    }

    @Test
    void shouldRejectCancellingAnOperationOfAnotherCreditLimit() {
        service.consumeForPurchase(consume("op-1", 20_000));
        CreditLimit other = creditLimits.save(CreditLimit.assign(
                CardId.of("660e8400-e29b-41d4-a716-446655440000"), Money.ofARS(100_000), 0.3, "test"));

        assertThatThrownBy(() -> service.cancelOperation(new CancelOperationCommand(other.getId(), "op-1")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(creditLimit.getConsumedCredit().getAmount()).isEqualTo(Money.ofARS(20_000));
    }

    private ConsumeCreditCommand consume(String operationId, long amount) {
        return new ConsumeCreditCommand(creditLimit.getId(), Money.ofARS(amount), "compra", operationId);
    }

    /**
     * Guarda las mismas instancias: lo que el servicio modifica se ve en el test
     */
    private static class InMemoryCreditLimits implements CreditLimitRepository {
        private final Map<String, CreditLimit> store = new HashMap<>();

        @Override
        public CreditLimit save(CreditLimit creditLimit) {
            store.put(creditLimit.getId(), creditLimit);
            return creditLimit;
        }

        @Override
        public Optional<CreditLimit> findById(String creditLimitId) {
            return Optional.ofNullable(store.get(creditLimitId));
        }

        @Override
        public Optional<CreditLimit> findByIdForUpdate(String creditLimitId) {
            return findById(creditLimitId);
        }

        @Override
        public Optional<CreditLimit> findByCardId(CardId cardId) {
            return store.values().stream().filter(c -> c.getCardId().equals(cardId)).findFirst();
        }

        @Override
        public boolean existsByCardId(CardId cardId) {
            return findByCardId(cardId).isPresent();
        }

        @Override
        public void delete(String creditLimitId) {
            store.remove(creditLimitId);
        }

        @Override
        public CreditLimitHistory saveHistory(CreditLimitHistory history) {
            return history;
        }

        @Override
        public List<CreditLimitHistory> findHistoryByCreditLimitId(String creditLimitId) {
            return List.of();
        }
    }

    private static class InMemoryOperations implements CreditOperationRepository {
        private final Map<String, CreditOperation> store = new HashMap<>();

        @Override
        public Optional<CreditOperation> findById(String operationId) {
            return Optional.ofNullable(store.get(operationId));
        }

        @Override
        public CreditOperation save(CreditOperation operation) {
            store.put(operation.getOperationId(), operation);
            return operation;
        }
    }
}
