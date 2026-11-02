package com.unisono.card.limits.infrastructure.adapter.output.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardJpaRepository extends JpaRepository<CardJpaEntity, String> {
    boolean existsById(String id);
}
