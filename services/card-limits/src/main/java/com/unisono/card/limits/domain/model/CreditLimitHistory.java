package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.valueobject.Money;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity: CreditLimitHistory
 *
 * Representa un registro histórico de cambios en los límites de crédito.
 * Pertenece al aggregate CreditLimit.
 *
 * Casos de uso:
 * - Auditoría de cambios
 * - Análisis de comportamiento crediticio
 * - Cumplimiento regulatorio
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreditLimitHistory {

    private Long id;
    private String creditLimitId;

    // Tipo de cambio
    private LimitType limitType;
    private String action; // INCREASED, DECREASED, ASSIGNED, UPDATED

    // Valores
    private Money previousValue;
    private Money newValue;
    private Money difference;

    // Contexto
    private String reason;
    private String changedBy; // Usuario/sistema que hizo el cambio

    // Timestamp
    private LocalDateTime changedAt;

    // Metadata adicional (JSON)
    private String metadata;

    // ========================================
    // FACTORY METHODS
    // ========================================

    /**
     * Registra una asignación inicial de límite
     */
    public static CreditLimitHistory recordAssignment(
            String creditLimitId,
            LimitType limitType,
            Money assignedValue,
            String reason,
            String changedBy
    ) {
        return CreditLimitHistory.builder()
                .creditLimitId(creditLimitId)
                .limitType(limitType)
                .action("ASSIGNED")
                .previousValue(Money.zero())
                .newValue(assignedValue)
                .difference(assignedValue)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Registra un aumento de límite
     */
    public static CreditLimitHistory recordIncrease(
            String creditLimitId,
            LimitType limitType,
            Money previousValue,
            Money newValue,
            String reason,
            String changedBy
    ) {
        Money difference = newValue.subtract(previousValue);

        return CreditLimitHistory.builder()
                .creditLimitId(creditLimitId)
                .limitType(limitType)
                .action("INCREASED")
                .previousValue(previousValue)
                .newValue(newValue)
                .difference(difference)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Registra una disminución de límite
     */
    public static CreditLimitHistory recordDecrease(
            String creditLimitId,
            LimitType limitType,
            Money previousValue,
            Money newValue,
            String reason,
            String changedBy
    ) {
        Money difference = previousValue.subtract(newValue);

        return CreditLimitHistory.builder()
                .creditLimitId(creditLimitId)
                .limitType(limitType)
                .action("DECREASED")
                .previousValue(previousValue)
                .newValue(newValue)
                .difference(difference)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Registra una actualización genérica
     */
    public static CreditLimitHistory recordUpdate(
            String creditLimitId,
            LimitType limitType,
            Money previousValue,
            Money newValue,
            String reason,
            String changedBy
    ) {
        Money difference = newValue.subtract(previousValue);
        String action = difference.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                ? "INCREASED"
                : "DECREASED";

        return CreditLimitHistory.builder()
                .creditLimitId(creditLimitId)
                .limitType(limitType)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .difference(Money.ofARS(difference.getAmount().abs()))
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Reconstruye desde persistencia
     */
    public static CreditLimitHistory reconstituteFromPersistence(
            Long id,
            String creditLimitId,
            LimitType limitType,
            String action,
            Money previousValue,
            Money newValue,
            Money difference,
            String reason,
            String changedBy,
            LocalDateTime changedAt,
            String metadata
    ) {
        return CreditLimitHistory.builder()
                .id(id)
                .creditLimitId(creditLimitId)
                .limitType(limitType)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .difference(difference)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .metadata(metadata)
                .build();
    }

    // ========================================
    // BUSINESS LOGIC
    // ========================================

    /**
     * Verifica si fue un aumento
     */
    public boolean wasIncrease() {
        return "INCREASED".equals(this.action);
    }

    /**
     * Verifica si fue una disminución
     */
    public boolean wasDecrease() {
        return "DECREASED".equals(this.action);
    }

    /**
     * Verifica si fue la asignación inicial
     */
    public boolean wasInitialAssignment() {
        return "ASSIGNED".equals(this.action);
    }

    /**
     * Obtiene una descripción legible del cambio
     */
    public String getDescription() {
        return String.format(
                "%s: %s cambió de %s a %s (diferencia: %s) - Razón: %s",
                limitType.getDisplayName(),
                action,
                previousValue,
                newValue,
                difference,
                reason != null ? reason : "N/A"
        );
    }

    /**
     * Agrega metadata adicional (JSON)
     */
    public void addMetadata(String metadata) {
        this.metadata = metadata;
    }

    // ========================================
    // EQUALS & HASHCODE
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditLimitHistory that = (CreditLimitHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "CreditLimitHistory{id=%s, action=%s, limitType=%s, previous=%s, new=%s, changedAt=%s}",
                id, action, limitType, previousValue, newValue, changedAt
        );
    }
}
