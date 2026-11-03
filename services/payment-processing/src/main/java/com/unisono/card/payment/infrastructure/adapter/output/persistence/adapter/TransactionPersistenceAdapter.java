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

        // New transactions carry version 0 from the domain; a null version makes
        // Spring Data persist them instead of merging a row that does not exist yet
        if (!jpaRepository.existsByTransactionId(entity.getTransactionId())) {
            entity.setVersion(null);
        }

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
    public Optional<Transaction> findByIdForUpdate(TransactionId transactionId) {
        return jpaRepository.findByIdForUpdate(transactionId.getValue())
                .map(entityMapper::toDomain);
    }

    @Override
    public List<Transaction> findRefundsOf(TransactionId originalTransactionId) {
        return jpaRepository.findByOriginalTransactionIdOrderByTransactionDateAsc(originalTransactionId.getValue())
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findPreAuthorizationsAuthorizedBefore(LocalDateTime date) {
        return jpaRepository.findExpiredPreAuthorizations(date)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCardId(CardId cardId) {
        log.debug("Finding transactions for card: {}", cardId);

        String cardIdValue = cardId.getValue();

        return jpaRepository.findByCardIdOrderByTransactionDateDesc(cardIdValue)
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCardIdAndStatus(CardId cardId, TransactionStatus status) {
        log.debug("Finding transactions for card: {} with status: {}", cardId, status);

        String cardIdValue = cardId.getValue();

        return jpaRepository.findByCardIdAndTransactionStatusOrderByTransactionDateDesc(
                        cardIdValue,
                        status
                )
                .stream()
                .map(entityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCardIdAndDateRange(CardId cardId, LocalDateTime from, LocalDateTime to) {
        log.debug("Finding transactions for card: {} between {} and {}", cardId, from, to);

        String cardIdValue = cardId.getValue();

        return jpaRepository.findByCardIdAndDateRange(cardIdValue, from, to)
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
}
