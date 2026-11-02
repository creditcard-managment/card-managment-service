package com.unisono.card.data.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository para OutboxEventEntity
 */
@Repository
public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Toma el próximo lote de eventos pendientes en orden de inserción.
     * FOR UPDATE SKIP LOCKED permite varias instancias del relay sin publicar dos veces el mismo lote.
     * Debe ejecutarse dentro de una transacción.
     */
    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL " +
            "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEventEntity> lockNextPendingBatch(@Param("limit") int limit);

    long countByPublishedAtIsNull();
}
