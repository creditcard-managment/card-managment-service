package com.unisono.card.payment.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity para payment_credit_operations (V12)
 *
 * Cada consumo o restauración de crédito pedido a card-limits. Se inserta PENDING (en su propia transacción)
 * antes de llamar y pasa a CONFIRMED o REJECTED en la transacción que guarda el pago. Si esa transacción
 * no confirma, queda PENDING y se cancela en card-limits (COMPENSATED).
 */
@Entity
@Table(name = "payment_credit_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreditOperationEntity {

    private static final int MAX_ERROR_LENGTH = 1000;

    public enum Type { CONSUME_PURCHASE, CONSUME_CASH_ADVANCE, RESTORE }

    public enum Status { PENDING, CONFIRMED, REJECTED, COMPENSATED }

    @Id
    @Column(name = "operation_id", length = 64)
    private String operationId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private Type operationType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markCompensated() {
        this.attempts++;
        this.status = Status.COMPENSATED;
        this.lastError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * La cancelación falló: sigue PENDING y se reintenta
     */
    public void markCompensationFailed(String error) {
        this.attempts++;
        this.lastError = error != null && error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
        this.updatedAt = LocalDateTime.now();
    }
}
