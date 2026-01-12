package com.unisono.card.payment.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.mapper.TransactionEntityMapper;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.repository.JpaTransactionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementing TransactionRepository port using JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;
    private final TransactionEntityMapper entityMapper;

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Saving transaction: {}", transaction.getTransactionId());

        TransactionEntity entity = entityMapper.toEntity(transaction);
        TransactionEntity savedEntity = jpaRepository.save(entity);

        return entityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Transaction> findById(TransactionId transactionId) {
        log.debug("Finding transaction by ID: {}", transactionId);

        return jpaRepository.findById(transactionId.getValue())
                .map(entityMapper::toDomain);
    }

    @Override
    public List<Transaction> findByCardId(CardId cardId) {
        log.debug("Finding transactions for card: {}", cardId);

        UUID cardUuid = toUuid(cardId.getValue()); // ✅ String → UUID

        return jpaRepository.findByCardIdOrderByTransactionDateDesc(cardUuid)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCardIdAndStatus(CardId cardId, TransactionStatus status) {
        log.debug("Finding transactions for card: {} with status: {}", cardId, status);

        UUID cardUuid = toUuid(cardId.getValue()); // ✅ String → UUID

        return jpaRepository.findByCardIdAndTransactionStatusOrderByTransactionDateDesc(
                        cardUuid,
                        status
                )
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCardIdAndDateRange(CardId cardId, LocalDateTime from, LocalDateTime to) {
        log.debug("Finding transactions for card: {} between {} and {}", cardId, from, to);

        UUID cardUuid = toUuid(cardId.getValue()); // ✅ String → UUID

        return jpaRepository.findByCardIdAndDateRange(cardUuid, from, to)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByStatus(TransactionStatus status) {
        log.debug("Finding transactions with status: {}", status);

        return jpaRepository.findByTransactionStatusOrderByTransactionDateDesc(status)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findPendingSettlement() {
        log.debug("Finding transactions pending settlement");

        return jpaRepository.findPendingSettlement()
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findCreatedBefore(LocalDateTime date) {
        log.debug("Finding transactions created before: {}", date);

        return jpaRepository.findCreatedBefore(date)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(TransactionId transactionId) {
        log.debug("Checking if transaction exists: {}", transactionId);

        return jpaRepository.existsByTransactionId(transactionId.getValue());
    }

    @Override
    public void delete(TransactionId transactionId) {
        log.debug("Deleting transaction: {}", transactionId);

        jpaRepository.deleteById(transactionId.getValue());
    }

    // ==================== Helper Methods ====================

    /**
     * Convert String to UUID
     */
    private UUID toUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", value, e);
            throw new IllegalArgumentException("Invalid CardId UUID format: " + value, e);
        }
    }
}
