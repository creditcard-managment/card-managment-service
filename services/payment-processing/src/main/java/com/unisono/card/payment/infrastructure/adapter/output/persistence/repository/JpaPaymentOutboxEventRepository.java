package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentOutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JpaPaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEventEntity, Long> {

    /**
     * Próximo lote de eventos pendientes en orden de inserción.
     * FOR UPDATE SKIP LOCKED permite varias instancias del relay sin publicar dos veces el mismo lote.
     * Debe ejecutarse dentro de una transacción.
     */
    @Query(value = "SELECT * FROM payment_outbox_events WHERE published_at IS NULL AND failed_at IS NULL " +
            "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<PaymentOutboxEventEntity> lockNextPendingBatch(@Param("limit") int limit);

    /**
     * Borra los eventos publicados antes de la fecha indicada (los fallidos se conservan)
     */
    @Modifying
    @Query("DELETE FROM PaymentOutboxEventEntity e WHERE e.publishedAt IS NOT NULL AND e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") LocalDateTime before);

    long countByFailedAtIsNotNull();

    long countByPublishedAtIsNullAndFailedAtIsNull();

    /**
     * Pendientes de publicar creados antes de {@code before}: el relay no logra publicarlos (broker caído)
     */
    long countByPublishedAtIsNullAndFailedAtIsNullAndCreatedAtBefore(LocalDateTime before);
}
