package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentCreditOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPaymentCreditOperationRepository extends JpaRepository<PaymentCreditOperationEntity, String> {

    /**
     * Bloquea la operación hasta el fin de la transacción actual. La transacción del pago la tiene tomada
     * mientras llama a card-limits: el reconciliador (SKIP LOCKED) no la compensa mientras siga en curso.
     */
    @Query(value = "SELECT * FROM payment_credit_operations WHERE operation_id = :operationId FOR UPDATE",
            nativeQuery = true)
    Optional<PaymentCreditOperationEntity> lockById(@Param("operationId") String operationId);

    /**
     * Operaciones PENDING más viejas que el umbral y que ninguna transacción tiene tomadas:
     * su pago no confirmó y hay que cancelarlas en card-limits. Debe ejecutarse dentro de una transacción.
     */
    @Query(value = "SELECT * FROM payment_credit_operations WHERE status = 'PENDING' AND created_at < :before " +
            "ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<PaymentCreditOperationEntity> lockStalePending(@Param("before") LocalDateTime before,
                                                        @Param("limit") int limit);

    @Modifying
    @Query("UPDATE PaymentCreditOperationEntity o SET o.status = :status, o.updatedAt = :now " +
            "WHERE o.operationId = :operationId AND o.status = 'PENDING'")
    int resolvePending(@Param("operationId") String operationId,
                       @Param("status") PaymentCreditOperationEntity.Status status,
                       @Param("now") LocalDateTime now);

    long countByStatus(PaymentCreditOperationEntity.Status status);
}
