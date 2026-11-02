package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.ValidationException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
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

        this.purchaseLimit = this.purchaseLimit.decrease(reductionAmount);

        // Validar que el nuevo límite no sea menor al crédito consumido
        if (this.purchaseLimit.getAmount().isLessThan(this.consumedCredit.getAmount())) {
            throw new BusinessRuleException(
                    "No se puede reducir el límite por debajo del crédito consumido actual"
            );
        }

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
        Money previousLimit = this.dailyLimit.getAmount();

        this.dailyLimit = this.dailyLimit.updateLimit(newDailyLimit);

        // ⭐ Registrar en historial
        this.history.add(CreditLimitHistory.recordUpdate(
                this.id,
                LimitType.DAILY,
                previousLimit,
                newDailyLimit,
                reason,
                changedBy
        ));

        touch();
    }

    /**
     * Actualiza el límite mensual
     */
    public void updateMonthlyLimit(Money newMonthlyLimit, String reason, String changedBy) {
        Money previousLimit = this.monthlyLimit.getAmount();

        this.monthlyLimit = this.monthlyLimit.updateLimit(newMonthlyLimit);

        // ⭐ Registrar en historial
        this.history.add(CreditLimitHistory.recordUpdate(
                this.id,
                LimitType.MONTHLY,
                previousLimit,
                newMonthlyLimit,
                reason,
                changedBy
        ));

        touch();
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
        if (paymentAmount == null || paymentAmount.isZero()) {
            throw new ValidationException("El monto del pago debe ser mayor a cero");
        }

        this.consumedCredit = this.consumedCredit.release(paymentAmount);
        recalculateAvailableCredit();
        touch();
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
