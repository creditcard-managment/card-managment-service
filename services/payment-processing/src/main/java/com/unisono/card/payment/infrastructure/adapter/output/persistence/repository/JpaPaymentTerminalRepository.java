package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.PaymentTerminalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for PaymentTerminalEntity (solo lectura)
 */
@Repository
public interface JpaPaymentTerminalRepository extends JpaRepository<PaymentTerminalEntity, String> {

    Optional<PaymentTerminalEntity> findByIdAndStatus(String id, String status);
}
