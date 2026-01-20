package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.repository;

import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantStatus;
import com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository para MerchantEntity
 */
@Repository
public interface JpaMerchantRepository extends JpaRepository<MerchantEntity, String> {

    Optional<MerchantEntity> findByTaxId(String taxId);

    Optional<MerchantEntity> findByEmail(String email);

    List<MerchantEntity> findByCategory(MerchantCategory category);

    List<MerchantEntity> findByStatus(MerchantStatus status);

    boolean existsByTaxId(String taxId);

    boolean existsByEmail(String email);

    long countByStatus(MerchantStatus status);

    @Query("SELECT m FROM MerchantEntity m WHERE LOWER(m.legalName) LIKE LOWER(CONCAT('%', :namePart, '%'))")
    List<MerchantEntity> findByLegalNameContaining(@Param("namePart") String namePart);

    @Query("SELECT m FROM MerchantEntity m WHERE m.status = 'ACTIVE'")
    List<MerchantEntity> findAllActive();
}
