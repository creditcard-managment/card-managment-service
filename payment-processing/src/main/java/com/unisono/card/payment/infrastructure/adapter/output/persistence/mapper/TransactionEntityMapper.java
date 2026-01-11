package com.unisono.card.payment.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.payment.domain.model.AuthorizationCode;
import com.unisono.card.payment.domain.model.CardData;
import com.unisono.card.payment.domain.model.MerchantInfo;
import com.unisono.card.payment.domain.model.PaymentMethod;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionAmount;
import com.unisono.card.payment.domain.model.TransactionId;
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
 * Mapper for converting between Transaction domain model and TransactionEntity
 * Uses manual mapping due to Transaction's private constructor
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

        return TransactionEntity.builder()
                .transactionId(transaction.getTransactionId().getValue())
                .cardId(stringToUuid(transaction.getCardId().getValue())) // ✅ String → UUID
                .merchantId(extractMerchantId(transaction.getMerchantInfo()))
                // ... resto del builder
                .build();
    }

    // Helper method
    private UUID stringToUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }


    /**
     * Convert TransactionEntity to Transaction domain model
     * Uses reflection to reconstruct the aggregate
     */
    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        // Reconstruct value objects
        TransactionId transactionId = mapToTransactionId(entity.getTransactionId());
        CardId cardId = mapToCardId(entity.getCardId());
        CardData cardData = mapToCardData(entity);
        MerchantInfo merchantInfo = mapToMerchantInfo(entity);
        TransactionAmount amount = mapToTransactionAmount(entity);

        // Create transaction based on type
        Transaction transaction;

        if (entity.getTransactionType() == TransactionType.REFUND) {
            // For refunds, we need the original transaction ID (not available in entity)
            // We'll create a purchase and then manually set fields
            transaction = createTransactionFromEntity(
                    transactionId, cardId, cardData, merchantInfo,
                    entity.getTransactionType(), amount, entity.getPaymentMethod()
            );
        } else {
            transaction = createTransactionFromEntity(
                    transactionId, cardId, cardData, merchantInfo,
                    entity.getTransactionType(), amount, entity.getPaymentMethod()
            );
        }

        // Reconstruct state using reflection
        reconstructTransactionState(transaction, entity);

        return transaction;
    }

    /**
     * Create a transaction instance using reflection
     */
    private Transaction createTransactionFromEntity(
            TransactionId transactionId,
            CardId cardId,
            CardData cardData,
            MerchantInfo merchantInfo,
            TransactionType type,
            TransactionAmount amount,
            PaymentMethod paymentMethod) {

        try {
            // Use reflection to access private constructor
            java.lang.reflect.Constructor<Transaction> constructor =
                    Transaction.class.getDeclaredConstructor(
                            TransactionId.class,
                            CardId.class,
                            CardData.class,
                            MerchantInfo.class,
                            TransactionType.class,
                            TransactionAmount.class,
                            PaymentMethod.class
                    );

            constructor.setAccessible(true);
            return constructor.newInstance(
                    transactionId, cardId, cardData, merchantInfo,
                    type, amount, paymentMethod
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct Transaction from entity", e);
        }
    }

    /**
     * Reconstruct transaction state using reflection
     */
    private void reconstructTransactionState(Transaction transaction, TransactionEntity entity) {
        try {
            setField(transaction, "status", entity.getTransactionStatus());
            setField(transaction, "authorizationCode", mapToAuthorizationCode(entity));
            setField(transaction, "riskScore", entity.getRiskScore());
            setField(transaction, "declineReason", entity.getDeclineReason());
            setField(transaction, "terminalId", entity.getTerminalId());
            setField(transaction, "posEntryMode", entity.getPosEntryMode());
            setField(transaction, "transactionDate", entity.getTransactionDate());
            setField(transaction, "settlementDate", entity.getSettlementDate());
            setField(transaction, "version", entity.getVersion());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct transaction state", e);
        }
    }

    /**
     * Set private field value using reflection
     */
    private void setField(Transaction transaction, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = Transaction.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(transaction, value);
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

    private CardId mapToCardId(UUID uuid) {
        return CardId.of(uuid.toString()); // ✅ UUID → String → CardId
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

    private UUID extractMerchantId(MerchantInfo merchantInfo) {
        try {
            return UUID.fromString(merchantInfo.getMerchantId());
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(merchantInfo.getMerchantId().getBytes());
        }
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
