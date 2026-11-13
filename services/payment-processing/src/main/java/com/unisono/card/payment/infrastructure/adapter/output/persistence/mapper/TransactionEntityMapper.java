package com.unisono.card.payment.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
import com.unisono.card.payment.domain.model.TransactionStatus;
import com.unisono.card.payment.domain.model.TransactionType;
import com.unisono.card.payment.infrastructure.adapter.output.persistence.entity.TransactionEntity;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Transaction domain model and TransactionEntity.
 * Rebuilds the aggregate with {@link Transaction#reconstitute}, without reflection.
 */
@Component
public class TransactionEntityMapper {

    /**
     * Convert Transaction domain model to TransactionEntity
     */
    public TransactionEntity toEntity(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        CardData cardData = transaction.getCardData();
        MerchantInfo merchantInfo = transaction.getMerchantInfo();
        Money amount = transaction.getAmount().getAmount();

        return TransactionEntity.builder()
                .transactionId(transaction.getTransactionId().getValue())
                .cardId(transaction.getCardId().toString())
                .merchantId(extractMerchantId(merchantInfo))
                .transactionType(transaction.getType())
                .transactionStatus(transaction.getStatus())
                .paymentMethod(transaction.getPaymentMethod())
                // Amount
                .amount(amount.getAmount())
                .currency(amount.getCurrencyCode())
                .localAmount(getLocalAmount(transaction))
                .localCurrency(getLocalCurrency(transaction))
                .exchangeRate(transaction.getAmount().getExchangeRate())
                // Card (tokenized)
                .cardNumberToken(cardData.getCardNumberToken())
                .cardLastFour(cardData.getLastFourDigits())
                .cardholderName(cardData.getCardholderName())
                .expirationDate(cardData.getExpirationDate().toString()) // MM/YYYY
                // Authorization
                .authorizationCode(getAuthorizationCode(transaction))
                .authorizationDate(getAuthorizationDate(transaction))
                .authorizationResponseCode(getAuthorizationResponseCode(transaction))
                // Merchant
                .merchantCategoryCode(merchantInfo.getMerchantCategoryCode())
                .merchantName(merchantInfo.getMerchantName())
                .merchantCity(merchantInfo.getCity())
                .merchantCountry(merchantInfo.getCountry())
                // Transaction details
                .terminalId(transaction.getTerminalId())
                .posEntryMode(transaction.getPosEntryMode())
                .transactionDate(transaction.getTransactionDate())
                .settlementDate(transaction.getSettlementDate())
                // Fraud & status
                .riskScore(transaction.getRiskScore())
                .declineReason(transaction.getDeclineReason())
                .reversalReason(transaction.getStatus() == TransactionStatus.REVERSED
                        ? transaction.getDeclineReason()
                        : null)
                .originalTransactionId(transaction.getOriginalTransactionId() != null
                        ? transaction.getOriginalTransactionId().getValue()
                        : null)
                .version(transaction.getVersion())
                .build();
    }


    /**
     * Convert TransactionEntity to Transaction domain model
     */
    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return Transaction.reconstitute(Transaction.PersistedState.builder()
                .transactionId(mapToTransactionId(entity.getTransactionId()))
                .cardId(mapToCardId(entity.getCardId()))
                .cardData(mapToCardData(entity))
                .merchantInfo(mapToMerchantInfo(entity))
                .type(entity.getTransactionType())
                .amount(mapToTransactionAmount(entity))
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getTransactionStatus())
                .authorizationCode(mapToAuthorizationCode(entity))
                .riskScore(entity.getRiskScore())
                .declineReason(entity.getDeclineReason())
                .terminalId(entity.getTerminalId())
                .posEntryMode(entity.getPosEntryMode())
                .transactionDate(entity.getTransactionDate())
                .settlementDate(entity.getSettlementDate())
                .originalTransactionId(entity.getOriginalTransactionId() != null
                        ? mapToTransactionId(entity.getOriginalTransactionId())
                        : null)
                .version(entity.getVersion() != null ? entity.getVersion() : 0)
                .build());
    }

    /**
     * Convert list of entities to domain models
     */
    public List<Transaction> toDomainList(List<TransactionEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== Helper Mapping Methods ====================


    private TransactionId mapToTransactionId(UUID uuid) {
        return TransactionId.of(uuid);
    }

    private CardId mapToCardId(String cardId) {
        return CardId.of(cardId);
    }
    private CardData mapToCardData(TransactionEntity entity) {
        ExpirationDate expirationDate = ExpirationDate.of(entity.getExpirationDate());

        return CardData.of(
                entity.getCardNumberToken(),
                entity.getCardLastFour(),
                entity.getCardholderName(),
                expirationDate
        );
    }

    private MerchantInfo mapToMerchantInfo(TransactionEntity entity) {
        return MerchantInfo.of(
                entity.getMerchantId().toString(),
                entity.getMerchantName(),
                entity.getMerchantCategoryCode(),
                entity.getMerchantCity(),
                entity.getMerchantCountry()
        );
    }

    private TransactionAmount mapToTransactionAmount(TransactionEntity entity) {
        Money amount = Money.of(entity.getAmount(), entity.getCurrency());

        if (entity.getLocalAmount() != null && entity.getLocalCurrency() != null
                && entity.getExchangeRate() != null) {
            Money localAmount = Money.of(entity.getLocalAmount(), entity.getLocalCurrency());
            return TransactionAmount.withConversion(amount, localAmount, entity.getExchangeRate());
        }

        return TransactionAmount.of(amount);
    }

    private AuthorizationCode mapToAuthorizationCode(TransactionEntity entity) {
        if (entity.getAuthorizationCode() == null) {
            return null;
        }

        LocalDateTime authorizedAt = entity.getAuthorizationDate() != null
                ? entity.getAuthorizationDate()
                : entity.getTransactionDate();

        String responseCode = entity.getAuthorizationResponseCode() != null
                ? entity.getAuthorizationResponseCode()
                : "00";

        return AuthorizationCode.of(
                entity.getAuthorizationCode(),
                authorizedAt,
                responseCode
        );
    }

    private String extractMerchantId(MerchantInfo merchantInfo) {
        return merchantInfo.getMerchantId();
    }

    private BigDecimal getLocalAmount(Transaction transaction) {
        if (transaction.getAmount().hasCurrencyConversion()) {
            return transaction.getAmount().getLocalAmount().getAmount();
        }
        return null;
    }

    private String getLocalCurrency(Transaction transaction) {
        if (transaction.getAmount().hasCurrencyConversion()) {
            return transaction.getAmount().getLocalAmount().getCurrency().getCurrencyCode();
        }
        return null;
    }

    private String getAuthorizationCode(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getCode()
                : null;
    }

    private LocalDateTime getAuthorizationDate(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getAuthorizedAt()
                : null;
    }

    private String getAuthorizationResponseCode(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getResponseCode()
                : null;
    }
}
