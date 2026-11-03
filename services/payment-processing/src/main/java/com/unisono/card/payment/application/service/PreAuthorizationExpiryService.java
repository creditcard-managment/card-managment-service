package com.unisono.card.payment.application.service;

import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.port.input.ReverseTransactionUseCase;
import com.unisono.card.payment.domain.port.output.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expires pre-authorizations that were neither captured nor reversed in time.
 * <p>
 * Each expired one is reversed through {@link ReverseTransactionUseCase}, in its own database transaction:
 * the authorization is voided, the held credit is given back and {@code PaymentReversed} is published.
 * A failure on one does not stop the others; it is retried on the next run.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreAuthorizationExpiryService {

    private final TransactionRepository transactionRepository;
    private final ReverseTransactionUseCase reverseTransactionUseCase;

    @Value("${payment.pre-authorization.expiration-hours:168}")
    private long expirationHours = 168;

    /**
     * @return how many pre-authorizations were expired
     */
    public int expireOverdue() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(expirationHours);
        List<Transaction> overdue = transactionRepository.findPreAuthorizationsAuthorizedBefore(threshold);
        if (overdue.isEmpty()) {
            return 0;
        }

        log.info("Expiring {} pre-authorizations authorized before {}", overdue.size(), threshold);
        String reason = String.format("Pre-authorization expired: not captured within %d hours", expirationHours);

        int expired = 0;
        for (Transaction preAuthorization : overdue) {
            try {
                reverseTransactionUseCase.reverseTransaction(preAuthorization.getTransactionId(), reason);
                expired++;
            } catch (Exception ex) {
                log.error("Could not expire pre-authorization {}, will retry on the next run",
                        preAuthorization.getTransactionId(), ex);
            }
        }

        log.info("Expired {}/{} pre-authorizations", expired, overdue.size());
        return expired;
    }
}
