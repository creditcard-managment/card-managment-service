package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.port.input.QueryTransactionUseCase;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Application service for querying transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService implements QueryTransactionUseCase {

    private final TransactionRepository transactionRepository;

    @Override
    public Optional<Transaction> findById(TransactionId transactionId) {
        log.debug("Finding transaction by ID: {}", transactionId);
        return transactionRepository.findById(transactionId);
    }

    @Override
    public List<Transaction> findByCardId(CardId cardId) {
        log.debug("Finding all transactions for card: {}", cardId);
        return transactionRepository.findByCardId(cardId);
    }

    @Override
    public List<Transaction> findByCardIdAndStatus(CardId cardId, TransactionStatus status) {
        log.debug("Finding transactions for card: {} with status: {}", cardId, status);
        return transactionRepository.findByCardIdAndStatus(cardId, status);
    }

    @Override
    public List<Transaction> findByCardIdAndDateRange(CardId cardId, LocalDateTime from, LocalDateTime to) {
        log.debug("Finding transactions for card: {} between {} and {}", cardId, from, to);
        return transactionRepository.findByCardIdAndDateRange(cardId, from, to);
    }

    @Override
    public List<Transaction> findPendingSettlement() {
        log.debug("Finding transactions pending settlement");
        return transactionRepository.findPendingSettlement();
    }
}
