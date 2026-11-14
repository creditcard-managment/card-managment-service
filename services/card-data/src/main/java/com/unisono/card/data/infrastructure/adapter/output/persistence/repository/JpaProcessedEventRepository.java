package com.unisono.card.data.infrastructure.adapter.output.persistence.repository;

import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
