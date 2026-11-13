package com.unisono.card.limits.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitHistoryEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.mapper.CreditLimitEntityMapper;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.repository.JpaCreditLimitHistoryRepository;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.repository.JpaCreditLimitRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para CreditLimit
 *
 * Implementa CreditLimitRepository (domain port) usando JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitRepositoryAdapter implements CreditLimitRepository {

    private final JpaCreditLimitRepository jpaRepository;
    private final JpaCreditLimitHistoryRepository jpaHistoryRepository;
    private final CreditLimitEntityMapper mapper;

    @Override
    public CreditLimit save(CreditLimit creditLimit) {
        log.debug("Saving credit limit: {}", creditLimit.getId());

        CreditLimitEntity entity = mapper.toEntity(creditLimit);
        CreditLimitEntity savedEntity = jpaRepository.save(entity);

        CreditLimit savedCreditLimit = mapper.toDomain(savedEntity);

        log.debug("Credit limit saved successfully: {}", savedCreditLimit.getId());

        return savedCreditLimit;
    }

    @Override
    public Optional<CreditLimit> findById(String creditLimitId) {
        log.debug("Finding credit limit by ID: {}", creditLimitId);

        return jpaRepository.findById(creditLimitId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CreditLimit> findByIdForUpdate(String creditLimitId) {
        log.debug("Locking credit limit: {}", creditLimitId);

        return jpaRepository.findByIdForUpdate(creditLimitId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CreditLimit> findByCardId(CardId cardId) {
        log.debug("Finding credit limit for card: {}", cardId);

        return jpaRepository.findByCardId(cardId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCardId(CardId cardId) {
        return jpaRepository.existsByCardId(cardId.getValue());
    }

    @Override
    public void delete(String creditLimitId) {
        log.debug("Deleting credit limit (soft delete): {}", creditLimitId);

        jpaRepository.findById(creditLimitId).ifPresent(entity -> {
            entity.markAsDeleted();
            jpaRepository.save(entity);
        });
    }

    @Override
    public CreditLimitHistory saveHistory(CreditLimitHistory history) {
        log.debug("Saving credit limit history: {}", history.getCreditLimitId());

        CreditLimitHistoryEntity entity = mapper.toHistoryEntity(history);
        CreditLimitHistoryEntity savedEntity = jpaHistoryRepository.save(entity);

        return mapper.toHistoryDomain(savedEntity);
    }

    @Override
    public List<CreditLimitHistory> findHistoryByCreditLimitId(String creditLimitId) {
        log.debug("Finding history for credit limit: {}", creditLimitId);

        return jpaHistoryRepository.findByCreditLimitIdOrderByChangedAtDesc(creditLimitId)
                .stream()
                .map(mapper::toHistoryDomain)
                .collect(Collectors.toList());
    }
}
