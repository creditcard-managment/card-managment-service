package com.unisono.card.limits.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCreditOperationRepository extends JpaRepository<CreditOperationEntity, String> {
}
