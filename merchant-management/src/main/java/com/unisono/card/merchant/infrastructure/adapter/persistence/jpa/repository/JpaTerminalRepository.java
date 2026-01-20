package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.repository;

import com.unisono.card.merchant.domain.model.terminal.TerminalStatus;
import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.PaymentTerminalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository para PaymentTerminalEntity
 */
@Repository
public interface JpaTerminalRepository extends JpaRepository<PaymentTerminalEntity, String> {

    Optional<PaymentTerminalEntity> findBySerialNumber(String serialNumber);

    @Query("SELECT t FROM PaymentTerminalEntity t WHERE t.merchant.id = :merchantId")
    List<PaymentTerminalEntity> findByMerchantId(@Param("merchantId") String merchantId);

    @Query("SELECT t FROM PaymentTerminalEntity t WHERE t.merchant.id = :merchantId AND t.status = :status")
    List<PaymentTerminalEntity> findByMerchantIdAndStatus(
            @Param("merchantId") String merchantId,
            @Param("status") TerminalStatus status
    );

    List<PaymentTerminalEntity> findByStatus(TerminalStatus status);

    @Query("SELECT t FROM PaymentTerminalEntity t WHERE TYPE(t) = :terminalClass")
    List<PaymentTerminalEntity> findByTerminalType(@Param("terminalClass") TerminalType type);

    boolean existsBySerialNumber(String serialNumber);

    @Query("SELECT COUNT(t) FROM PaymentTerminalEntity t WHERE t.merchant.id = :merchantId AND t.status = :status")
    long countByMerchantIdAndStatus(
            @Param("merchantId") String merchantId,
            @Param("status") TerminalStatus status
    );
}
