package com.unisono.card.limits.domain.model;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Operación de crédito identificada por el cliente (operationId).
 * <p>
 * Hace idempotentes los consumos y restauraciones: una operación ya aplicada no se vuelve a aplicar.
 * Guarda cuánto movió en lo consumido, en el cupo diario y en el mensual (positivo = consumió,
 * negativo = devolvió) para poder deshacerla exactamente con {@link CreditLimit#revertOperation}.
 * <p>
 * Estados:
 * <ul>
 *   <li>{@code APPLIED}: aplicada.</li>
 *   <li>{@code CANCELLED}: aplicada y después deshecha.</li>
 *   <li>{@code VOIDED}: se canceló antes de llegar. Si llega después, se rechaza.</li>
 * </ul>
 */
@Getter
public class CreditOperation {

    public enum Type { CONSUME_PURCHASE, CONSUME_CASH_ADVANCE, RESTORE }

    public enum Status { APPLIED, CANCELLED, VOIDED }

    private final String operationId;
    private final String creditLimitId;
    private final Type type;
    private Status status;
    private final Money creditDelta;
    private final Money dailyDelta;
    private final Money monthlyDelta;
    private final LocalDate appliedOn;
    private final String description;
    private final LocalDateTime createdAt;
    private LocalDateTime cancelledAt;

    private CreditOperation(String operationId, String creditLimitId, Type type, Status status,
                            Money creditDelta, Money dailyDelta, Money monthlyDelta, LocalDate appliedOn,
                            String description, LocalDateTime createdAt, LocalDateTime cancelledAt) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.creditLimitId = Objects.requireNonNull(creditLimitId, "creditLimitId");
        this.type = type;
        this.status = Objects.requireNonNull(status, "status");
        this.creditDelta = Objects.requireNonNull(creditDelta, "creditDelta");
        this.dailyDelta = Objects.requireNonNull(dailyDelta, "dailyDelta");
        this.monthlyDelta = Objects.requireNonNull(monthlyDelta, "monthlyDelta");
        this.appliedOn = Objects.requireNonNull(appliedOn, "appliedOn");
        this.description = description;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Operación recién aplicada, con lo que movió entre el antes y el después
     */
    public static CreditOperation applied(String operationId, String creditLimitId, Type type,
                                          CreditLimit.ConsumptionSnapshot before,
                                          CreditLimit.ConsumptionSnapshot after, String description) {
        return new CreditOperation(operationId, creditLimitId, Objects.requireNonNull(type, "type"), Status.APPLIED,
                after.credit().subtract(before.credit()),
                after.daily().subtract(before.daily()),
                after.monthly().subtract(before.monthly()),
                LocalDate.now(), description, LocalDateTime.now(), null);
    }

    /**
     * Cancelación de una operación que nunca llegó: no mueve nada y bloquea que se aplique después
     */
    public static CreditOperation voided(String operationId, String creditLimitId, String currency) {
        Money zero = Money.zero(currency);
        LocalDateTime now = LocalDateTime.now();
        return new CreditOperation(operationId, creditLimitId, null, Status.VOIDED,
                zero, zero, zero, now.toLocalDate(), null, now, now);
    }

    public static CreditOperation reconstitute(String operationId, String creditLimitId, Type type, Status status,
                                               Money creditDelta, Money dailyDelta, Money monthlyDelta,
                                               LocalDate appliedOn, String description,
                                               LocalDateTime createdAt, LocalDateTime cancelledAt) {
        return new CreditOperation(operationId, creditLimitId, type, status, creditDelta, dailyDelta, monthlyDelta,
                appliedOn, description, createdAt, cancelledAt);
    }

    public boolean isApplied() {
        return status == Status.APPLIED;
    }

    /**
     * Marca como deshecha una operación aplicada (el crédito se revierte en {@link CreditLimit#revertOperation})
     */
    public void cancel() {
        if (status != Status.APPLIED) {
            throw new BusinessRuleException(String.format(
                    "La operación %s no está aplicada (estado %s)", operationId, status));
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}
