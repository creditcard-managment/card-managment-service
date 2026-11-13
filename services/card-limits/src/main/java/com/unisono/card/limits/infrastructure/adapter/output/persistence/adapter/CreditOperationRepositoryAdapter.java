package com.unisono.card.limits.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.limits.domain.model.CreditOperation;
import com.unisono.card.limits.domain.port.output.CreditOperationRepository;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditOperationEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.repository.JpaCreditOperationRepository;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de persistencia de {@link CreditOperation}
 */
@Component
@RequiredArgsConstructor
public class CreditOperationRepositoryAdapter implements CreditOperationRepository {

    private final JpaCreditOperationRepository jpaRepository;

    @Override
    public Optional<CreditOperation> findById(String operationId) {
        return jpaRepository.findById(operationId).map(CreditOperationRepositoryAdapter::toDomain);
    }

    @Override
    public CreditOperation save(CreditOperation operation) {
        return toDomain(jpaRepository.save(toEntity(operation)));
    }

    static CreditOperationEntity toEntity(CreditOperation operation) {
        return CreditOperationEntity.builder()
                .operationId(operation.getOperationId())
                .creditLimitId(operation.getCreditLimitId())
                .operationType(operation.getType())
                .status(operation.getStatus())
                .creditDelta(operation.getCreditDelta().getAmount())
                .dailyDelta(operation.getDailyDelta().getAmount())
                .monthlyDelta(operation.getMonthlyDelta().getAmount())
                .currency(operation.getCreditDelta().getCurrencyCode())
                .appliedOn(operation.getAppliedOn())
                .description(operation.getDescription())
                .createdAt(operation.getCreatedAt())
                .cancelledAt(operation.getCancelledAt())
                .build();
    }

    static CreditOperation toDomain(CreditOperationEntity entity) {
        String currency = entity.getCurrency();
        return CreditOperation.reconstitute(
                entity.getOperationId(),
                entity.getCreditLimitId(),
                entity.getOperationType(),
                entity.getStatus(),
                Money.of(entity.getCreditDelta(), currency),
                Money.of(entity.getDailyDelta(), currency),
                Money.of(entity.getMonthlyDelta(), currency),
                entity.getAppliedOn(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getCancelledAt()
        );
    }
}
