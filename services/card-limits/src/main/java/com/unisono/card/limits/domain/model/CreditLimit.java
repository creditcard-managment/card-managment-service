package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root: CreditLimit
 *
 * Representa los límites de crédito de una tarjeta.
 *
 * Invariantes:
 * - El límite de adelantos no puede exceder el límite de compras
 * - El crédito consumido no puede exceder el límite total
 * - El crédito disponible = límite total - crédito consumido
 * - Los límites diarios/mensuales se auto-resetean
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreditLimit {

    private String id;
    private CardId cardId;

    // Límites principales
    private PurchaseLimit purchaseLimit;
    private CashAdvanceLimit cashAdvanceLimit;

    // Crédito actual
    private ConsumedCredit consumedCredit;
    private AvailableCredit availableCredit;

    // Límites temporales
    private DailyLimit dailyLimit;
    private MonthlyLimit monthlyLimit;
    @Builder.Default
    private List<CreditLimitHistory> history = new ArrayList<>();
    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // FACTORY METHODS
    // ========================================

    /**
     * Asigna un nuevo límite de crédito a una tarjeta
     */
    public static CreditLimit assign(
            CardId cardId,
            Money purchaseLimitAmount,
            double cashAdvancePercentage,
            String assignedBy
    ) {
        validateAssignment(cardId, purchaseLimitAmount, cashAdvancePercentage);

        PurchaseLimit purchaseLimit = PurchaseLimit.of(purchaseLimitAmount);
        CashAdvanceLimit cashAdvanceLimit = CashAdvanceLimit.fromPurchaseLimit(
                purchaseLimit,
                cashAdvancePercentage
        );

        LocalDateTime now = LocalDateTime.now();
        String creditLimitId = java.util.UUID.randomUUID().toString();

        // ⭐ Crear registro histórico inicial
        List<CreditLimitHistory> initialHistory = new ArrayList<>();
        initialHistory.add(CreditLimitHistory.recordAssignment(
                creditLimitId,
                LimitType.PURCHASE,
                purchaseLimitAmount,
                "Asignación inicial de límite",
                assignedBy
        ));

        return CreditLimit.builder()
                .id(creditLimitId)
                .cardId(cardId)
                .purchaseLimit(purchaseLimit)
                .cashAdvanceLimit(cashAdvanceLimit)
                .consumedCredit(ConsumedCredit.zero())
                .availableCredit(AvailableCredit.of(purchaseLimitAmount))
                .dailyLimit(DailyLimit.defaultLimit())
                .monthlyLimit(MonthlyLimit.defaultLimit())
                .history(initialHistory)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Reconstruye desde persistencia
     */
    public static CreditLimit reconstituteFromPersistence(
            String id,
            CardId cardId,
            PurchaseLimit purchaseLimit,
            CashAdvanceLimit cashAdvanceLimit,
            ConsumedCredit consumedCredit,
            AvailableCredit availableCredit,
            DailyLimit dailyLimit,
            MonthlyLimit monthlyLimit,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return CreditLimit.builder()
                .id(id)
                .cardId(cardId)
                .purchaseLimit(purchaseLimit)
                .cashAdvanceLimit(cashAdvanceLimit)
                .consumedCredit(consumedCredit)
                .availableCredit(availableCredit)
                .dailyLimit(dailyLimit)
                .monthlyLimit(monthlyLimit)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // ========================================
    // BUSINESS LOGIC - UPDATE LIMITS
    // ========================================

    /**
     * Aumenta el límite de compras
     */
    public void increasePurchaseLimit(Money additionalAmount, String reason, String changedBy) {
        Money previousLimit = this.purchaseLimit.getAmount();

        this.purchaseLimit = this.purchaseLimit.increase(additionalAmount);

        // ⭐ Registrar en historial
        this.history.add(CreditLimitHistory.recordIncrease(
                this.id,
                LimitType.PURCHASE,
                previousLimit,
                this.purchaseLimit.getAmount(),
                reason,
                changedBy
        ));

        recalculateAvailableCredit();
        touch();
    }

    /**
     * Disminuye el límite de compras
     */
    public void decreasePurchaseLimit(Money reductionAmount, String reason, String changedBy) {
        Money previousLimit = this.purchaseLimit.getAmount();

        PurchaseLimit newLimit = this.purchaseLimit.decrease(reductionAmount);

        // Validar que el nuevo límite no sea menor al crédito consumido
        if (newLimit.getAmount().isLessThan(this.consumedCredit.getAmount())) {
            throw new BusinessRuleException(
                    "No se puede reducir el límite por debajo del crédito consumido actual"
            );
        }

        // Invariante: el límite de adelantos no puede exceder el de compras
        if (this.cashAdvanceLimit.getAmount().isGreaterThan(newLimit.getAmount())) {
            throw new BusinessRuleException(String.format(
                    "No se puede reducir el límite de compras a %s: es menor al límite de adelantos de %s. "
                            + "Reducir primero el límite de adelantos",
                    newLimit.getAmount(), this.cashAdvanceLimit.getAmount()));
        }

        this.purchaseLimit = newLimit;

        // ⭐ Registrar en historial
        this.history.add(CreditLimitHistory.recordDecrease(
                this.id,
                LimitType.PURCHASE,
                previousLimit,
                this.purchaseLimit.getAmount(),
                reason,
                changedBy
        ));

        recalculateAvailableCredit();
        touch();
    }

    /**
     * Actualiza el límite de adelantos
     */
    public void updateCashAdvanceLimit(Money newAmount, String reason, String changedBy) {
        Money previousLimit = this.cashAdvanceLimit.getAmount();

        // Validar que no exceda el límite de compras
        if (newAmount.isGreaterThan(this.purchaseLimit.getAmount())) {
            throw new BusinessRuleException(
                    "El límite de adelantos no puede exceder el límite de compras"
            );
        }

        this.cashAdvanceLimit = CashAdvanceLimit.of(newAmount);

        // ⭐ Registrar en historial
        this.history.add(CreditLimitHistory.recordUpdate(
                this.id,
                LimitType.CASH_ADVANCE,
                previousLimit,
                newAmount,
                reason,
                changedBy
        ));

        touch();
    }

    /**
     * Actualiza el límite diario
     */
    public void updateDailyLimit(Money newDailyLimit, String reason, String changedBy) {
        updateTemporalLimits(newDailyLimit, null, reason, changedBy);
    }

    /**
     * Actualiza el límite mensual
     */
    public void updateMonthlyLimit(Money newMonthlyLimit, String reason, String changedBy) {
        updateTemporalLimits(null, newMonthlyLimit, reason, changedBy);
    }

    /**
     * Actualiza el límite diario y/o el mensual (null = no cambia). Valida la combinación final:
     * ambos mayores a cero y diario ≤ mensual. Así se pueden subir los dos a la vez aunque el diario nuevo
     * supere al mensual anterior.
     */
    public void updateTemporalLimits(Money newDailyLimit, Money newMonthlyLimit, String reason, String changedBy) {
        Money finalDaily = newDailyLimit != null ? newDailyLimit : this.dailyLimit.getAmount();
        Money finalMonthly = newMonthlyLimit != null ? newMonthlyLimit : this.monthlyLimit.getAmount();
        validateTemporalLimitAmount(newDailyLimit, "diario");
        validateTemporalLimitAmount(newMonthlyLimit, "mensual");
        if (finalDaily.isGreaterThan(finalMonthly)) {
            throw new BusinessRuleException(String.format(
                    "El límite diario (%s) no puede superar al mensual (%s)", finalDaily, finalMonthly));
        }

        if (newDailyLimit != null) {
            Money previousLimit = this.dailyLimit.getAmount();
            this.dailyLimit = this.dailyLimit.updateLimit(newDailyLimit);
            this.history.add(CreditLimitHistory.recordUpdate(
                    this.id, LimitType.DAILY, previousLimit, newDailyLimit, reason, changedBy));
        }

        if (newMonthlyLimit != null) {
            Money previousLimit = this.monthlyLimit.getAmount();
            this.monthlyLimit = this.monthlyLimit.updateLimit(newMonthlyLimit);
            this.history.add(CreditLimitHistory.recordUpdate(
                    this.id, LimitType.MONTHLY, previousLimit, newMonthlyLimit, reason, changedBy));
        }

        touch();
    }

    private static void validateTemporalLimitAmount(Money amount, String label) {
        if (amount != null && !amount.isPositive()) {
            throw new ValidationException(String.format("El límite %s debe ser mayor a cero", label));
        }
    }


    // ========================================
    // BUSINESS LOGIC - CONSUME CREDIT
    // ========================================

    /**
     * Consume crédito para una compra
     */
    public void consumeForPurchase(Money purchaseAmount) {
        validatePurchaseAmount(purchaseAmount);

        // Validar límite de compras
        if (!this.purchaseLimit.allows(purchaseAmount)) {
            throw new BusinessRuleException(
                    String.format("La compra de %s excede el límite de compras de %s",
                            purchaseAmount, this.purchaseLimit.getAmount())
            );
        }

        // Validar crédito disponible
        if (!this.availableCredit.isSufficientFor(purchaseAmount)) {
            throw new BusinessRuleException(
                    String.format("Crédito insuficiente. Disponible: %s, Solicitado: %s",
                            this.availableCredit.getAmount(), purchaseAmount)
            );
        }

        // Validar límites temporales
        validateTemporalLimits(purchaseAmount);

        // Consumir crédito
        this.consumedCredit = this.consumedCredit.consume(purchaseAmount);
        this.dailyLimit = this.dailyLimit.consume(purchaseAmount);
        this.monthlyLimit = this.monthlyLimit.consume(purchaseAmount);

        recalculateAvailableCredit();
        touch();
    }

    /**
     * Consume crédito para un adelanto en efectivo
     */
    public void consumeForCashAdvance(Money advanceAmount) {
        validatePurchaseAmount(advanceAmount);

        // Validar límite de adelantos
        if (!this.cashAdvanceLimit.allows(advanceAmount)) {
            throw new BusinessRuleException(
                    String.format("El adelanto de %s excede el límite de adelantos de %s",
                            advanceAmount, this.cashAdvanceLimit.getAmount())
            );
        }

        // Validar crédito disponible
        if (!this.availableCredit.isSufficientFor(advanceAmount)) {
            throw new BusinessRuleException(
                    String.format("Crédito insuficiente. Disponible: %s, Solicitado: %s",
                            this.availableCredit.getAmount(), advanceAmount)
            );
        }

        // Validar límites temporales
        validateTemporalLimits(advanceAmount);

        // Consumir crédito
        this.consumedCredit = this.consumedCredit.consume(advanceAmount);
        this.dailyLimit = this.dailyLimit.consume(advanceAmount);
        this.monthlyLimit = this.monthlyLimit.consume(advanceAmount);

        recalculateAvailableCredit();
        touch();
    }

    /**
     * Libera crédito (ej: después de un pago)
     */
    public void releaseCredit(Money paymentAmount) {
        if (paymentAmount == null || !paymentAmount.isPositive()) {
            throw new ValidationException("El monto del pago debe ser mayor a cero");
        }
        // No se modela saldo a favor: un pago no puede liberar más de lo consumido
        if (paymentAmount.isGreaterThan(this.consumedCredit.getAmount())) {
            throw new BusinessRuleException(String.format(
                    "El pago de %s supera el crédito consumido de %s",
                    paymentAmount, this.consumedCredit.getAmount()));
        }

        this.consumedCredit = this.consumedCredit.release(paymentAmount);
        recalculateAvailableCredit();
        touch();
    }

    /**
     * Deshace el consumo de una transacción reversada o devuelta.
     * <p>
     * A diferencia de {@link #releaseCredit} (pago del resumen), devuelve también el cupo diario y mensual
     * cuando la transacción original es del día o del mes en curso. Lo consumido baja a lo sumo hasta cero:
     * si la tarjeta ya pagó ese consumo, el excedente no se restaura (no se modela saldo a favor).
     *
     * @param amount       monto de la transacción reversada o devuelta
     * @param originalDate fecha de la transacción original (la que consumió el cupo)
     * @return el monto que no se pudo restaurar en lo consumido (cero si se restauró todo)
     */
    public Money restoreConsumption(Money amount, LocalDate originalDate) {
        if (amount == null || amount.isZero() || amount.isNegative()) {
            throw new ValidationException("El monto a restaurar debe ser mayor a cero");
        }
        if (originalDate == null) {
            throw new ValidationException("La fecha de la transacción original es obligatoria");
        }

        Money consumed = this.consumedCredit.getAmount();
        Money restorable = amount.isGreaterThan(consumed) ? consumed : amount;
        Money notRestored = amount.subtract(restorable);

        if (!restorable.isZero()) {
            this.consumedCredit = this.consumedCredit.release(restorable);
        }
        this.dailyLimit = this.dailyLimit.restore(amount, originalDate);
        this.monthlyLimit = this.monthlyLimit.restore(amount, YearMonth.from(originalDate));

        recalculateAvailableCredit();
        touch();
        return notRestored;
    }

    /**
     * Lo consumido, el cupo diario y el mensual en este momento. La diferencia entre dos fotos es lo que
     * movió una operación ({@link CreditOperation}).
     */
    public ConsumptionSnapshot consumptionSnapshot() {
        return new ConsumptionSnapshot(
                this.consumedCredit.getAmount(),
                this.dailyLimit.currentConsumed(),
                this.monthlyLimit.currentConsumed()
        );
    }

    /**
     * Deshace exactamente lo que movió una operación: lo consumido y, si siguen siendo del mismo día o mes,
     * el cupo diario y el mensual. Lo consumido no baja de cero.
     * <p>
     * No valida límites, a propósito: deshacer una restauración significa que el reverso o la devolución nunca
     * ocurrieron, así que lo restaurado se vuelve a consumir aunque la tarjeta haya gastado ese disponible
     * mientras tanto. En ese caso queda excedida ({@link #getOverLimitAmount()}) y las compras siguientes se
     * rechazan por crédito insuficiente hasta que pague.
     */
    public void revertOperation(CreditOperation operation) {
        Money consumed = this.consumedCredit.getAmount().subtract(operation.getCreditDelta());
        if (consumed.isNegative()) {
            consumed = Money.zero(consumed.getCurrencyCode());
        }
        this.consumedCredit = ConsumedCredit.of(consumed);
        this.dailyLimit = this.dailyLimit.revert(operation.getDailyDelta(), operation.getAppliedOn());
        this.monthlyLimit = this.monthlyLimit.revert(operation.getMonthlyDelta(), YearMonth.from(operation.getAppliedOn()));

        recalculateAvailableCredit();
        touch();
    }

    /**
     * Cuánto supera lo consumido al límite de compras (cero si no está excedida). Solo puede pasar al deshacer
     * una restauración ({@link #revertOperation}); los consumos normales validan el disponible.
     */
    public Money getOverLimitAmount() {
        Money available = this.availableCredit.getAmount();
        return available.isNegative() ? Money.zero(available.getCurrencyCode()).subtract(available)
                : Money.zero(available.getCurrencyCode());
    }

    public String getCurrencyCode() {
        return this.purchaseLimit.getAmount().getCurrencyCode();
    }

    public record ConsumptionSnapshot(Money credit, Money daily, Money monthly) {
    }

    // ========================================
    // BUSINESS LOGIC - VALIDATIONS
    // ========================================

    /**
     * Valida si una compra puede ser realizada
     */
    public boolean canPurchase(Money purchaseAmount) {
        try {
            validatePurchaseAmount(purchaseAmount);

            return this.purchaseLimit.allows(purchaseAmount)
                    && this.availableCredit.isSufficientFor(purchaseAmount)
                    && this.dailyLimit.allows(purchaseAmount)
                    && this.monthlyLimit.allows(purchaseAmount);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida si un adelanto puede ser realizado
     */
    public boolean canCashAdvance(Money advanceAmount) {
        try {
            validatePurchaseAmount(advanceAmount);

            return this.cashAdvanceLimit.allows(advanceAmount)
                    && this.availableCredit.isSufficientFor(advanceAmount)
                    && this.dailyLimit.allows(advanceAmount)
                    && this.monthlyLimit.allows(advanceAmount);
        } catch (Exception e) {
            return false;
        }
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    private void recalculateAvailableCredit() {
        this.availableCredit = AvailableCredit.calculate(
                this.purchaseLimit.getAmount(),
                this.consumedCredit.getAmount()
        );
    }

    private void validateTemporalLimits(Money amount) {
        if (!this.dailyLimit.allows(amount)) {
            throw new BusinessRuleException(
                    String.format("Se excede el límite diario. Disponible hoy: %s",
                            this.dailyLimit.getAvailable())
            );
        }

        if (!this.monthlyLimit.allows(amount)) {
            throw new BusinessRuleException(
                    String.format("Se excede el límite mensual. Disponible este mes: %s",
                            this.monthlyLimit.getAvailable())
            );
        }
    }

    private static void validateAssignment(
            CardId cardId,
            Money purchaseLimitAmount,
            double cashAdvancePercentage
    ) {
        if (cardId == null) {
            throw new ValidationException("El CardId es obligatorio");
        }
        if (purchaseLimitAmount == null) {
            throw new ValidationException("El límite de compras es obligatorio");
        }
        if (cashAdvancePercentage < 0 || cashAdvancePercentage > 1) {
            throw new ValidationException("El porcentaje de adelantos debe estar entre 0 y 1");
        }
    }

    private void validatePurchaseAmount(Money amount) {
        if (amount == null || amount.isZero()) {
            throw new ValidationException("El monto debe ser mayor a cero");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // EQUALS & HASHCODE
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditLimit that = (CreditLimit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "CreditLimit{id=%s, cardId=%s, purchaseLimit=%s, available=%s}",
                id, cardId, purchaseLimit.getAmount(), availableCredit.getAmount()
        );
    }


    // ========================================
    // HISTORY METHODS
    // ========================================

    /**
     * Obtiene el historial completo (inmutable)
     */
    public List<CreditLimitHistory> getHistory() {
        return Collections.unmodifiableList(this.history);
    }

    /**
     * Obtiene el historial filtrado por tipo de límite
     */
    public List<CreditLimitHistory> getHistoryByType(LimitType limitType) {
        return this.history.stream()
                .filter(h -> h.getLimitType() == limitType)
                .toList();
    }

    /**
     * Obtiene el último cambio registrado
     */
    public CreditLimitHistory getLastChange() {
        if (this.history.isEmpty()) {
            return null;
        }
        return this.history.get(this.history.size() - 1);
    }

    /**
     * Cuenta cuántos cambios se han hecho
     */
    public int getChangeCount() {
        return this.history.size();
    }

}
