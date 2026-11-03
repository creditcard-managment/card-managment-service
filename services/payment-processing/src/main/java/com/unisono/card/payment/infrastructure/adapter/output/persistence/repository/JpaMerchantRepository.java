package com.unisono.card.payment.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for MerchantEntity (solo lectura)
 */
@Repository
public interface JpaMerchantRepository extends JpaRepository<MerchantEntity, String> {

    boolean existsByIdAndStatus(String id, String status);
}
