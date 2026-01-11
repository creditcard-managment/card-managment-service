package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.response.TransactionResponse;
import com.unisono.card.payment.application.dto.response.TransactionSummaryResponse;
import com.unisono.card.payment.domain.model.Transaction;
import com.unisono.card.payment.domain.model.TransactionStatus;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Mapper for converting Transaction domain model to response DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransactionMapper {

    /**
     * Convert Transaction to TransactionResponse
     */
    @Mapping(target = "transactionId", expression = "java(transaction.getTransactionId().toString())")
    @Mapping(target = "cardId", expression = "java(transaction.getCardId().toString())")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")

    // Amount mapping
    @Mapping(target = "amount", expression = "java(transaction.getAmount().getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(transaction.getAmount().getAmount().getCurrency())")
    @Mapping(target = "localAmount", expression = "java(getLocalAmount(transaction))")
    @Mapping(target = "localCurrency", expression = "java(getLocalCurrency(transaction))")
    @Mapping(target = "exchangeRate", expression = "java(getExchangeRate(transaction))")

    // Card info (masked)
    @Mapping(target = "cardLastFour", expression = "java(transaction.getCardData().getLastFourDigits())")
    @Mapping(target = "cardholderName", expression = "java(transaction.getCardData().getCardholderName())")
    @Mapping(target = "cardExpirationDate", expression = "java(transaction.getCardData().getExpirationDate().getValue())")

    // Authorization
    @Mapping(target = "authorizationCode", expression = "java(getAuthorizationCode(transaction))")
    @Mapping(target = "authorizationResponseCode", expression = "java(getAuthorizationResponseCode(transaction))")
    @Mapping(target = "riskScore", source = "riskScore")
    @Mapping(target = "authorizedAt", expression = "java(getAuthorizedAt(transaction))")

    // Merchant info
    @Mapping(target = "merchantId", expression = "java(transaction.getMerchantInfo().getMerchantId())")
    @Mapping(target = "merchantName", expression = "java(transaction.getMerchantInfo().getMerchantName())")
    @Mapping(target = "merchantCategoryCode", expression = "java(transaction.getMerchantInfo().getMerchantCategoryCode())")
    @Mapping(target = "merchantCity", expression = "java(transaction.getMerchantInfo().getCity())")
    @Mapping(target = "merchantCountry", expression = "java(transaction.getMerchantInfo().getCountry())")

    // Transaction details
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "terminalId", source = "terminalId")
    @Mapping(target = "posEntryMode", source = "posEntryMode")
    @Mapping(target = "transactionDate", source = "transactionDate")
    @Mapping(target = "settlementDate", source = "settlementDate")

    // Status info
    @Mapping(target = "declineReason", source = "declineReason")

    // Metadata
    @Mapping(target = "version", source = "version")
    TransactionResponse toTransactionResponse(Transaction transaction);

    /**
     * Convert list of transactions to list of responses
     */
    List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions);

    /**
     * Create transaction summary from list of transactions
     */
    default TransactionSummaryResponse toTransactionSummary(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return TransactionSummaryResponse.builder()
                    .totalTransactions(0L)
                    .approvedCount(0L)
                    .declinedCount(0L)
                    .reversedCount(0L)
                    .pendingCount(0L)
                    .totalAmount(BigDecimal.ZERO)
                    .currency("USD")
                    .build();
        }

        long approvedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED || t.getStatus() == TransactionStatus.SETTLED)
                .count();

        long declinedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.DECLINED)
                .count();

        long reversedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.REVERSED)
                .count();

        long pendingCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING || t.getStatus() == TransactionStatus.AUTHORIZED)
                .count();

        BigDecimal totalAmount = transactions.stream()
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED || t.getStatus() == TransactionStatus.SETTLED)
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal declinedAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.DECLINED)
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = transactions.get(0).getAmount().getEffectiveAmount().getCurrency().toString();

        BigDecimal avgAmount = totalAmount.divide(
                BigDecimal.valueOf(transactions.size()),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal highestAmount = transactions.stream()
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowestAmount = transactions.stream()
                .map(t -> t.getAmount().getEffectiveAmount().getAmount())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return TransactionSummaryResponse.builder()
                .totalTransactions((long) transactions.size())
                .approvedCount(approvedCount)
                .declinedCount(declinedCount)
                .reversedCount(reversedCount)
                .pendingCount(pendingCount)
                .totalAmount(totalAmount)
                .currency(currency)
                .approvedAmount(approvedAmount)
                .declinedAmount(declinedAmount)
                .averageTransactionAmount(avgAmount.doubleValue())
                .highestTransactionAmount(highestAmount)
                .lowestTransactionAmount(lowestAmount)
                .build();
    }

    // ==================== Helper methods ====================

    default BigDecimal getLocalAmount(Transaction transaction) {
        return transaction.getAmount().hasCurrencyConversion()
                ? transaction.getAmount().getLocalAmount().getAmount()
                : null;
    }

    default String getLocalCurrency(Transaction transaction) {
        return transaction.getAmount().hasCurrencyConversion()
                ? transaction.getAmount().getLocalAmount().getCurrency().toString()
                : null;
    }

    default BigDecimal getExchangeRate(Transaction transaction) {
        return transaction.getAmount().getExchangeRate();
    }

    default String getAuthorizationCode(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getCode()
                : null;
    }

    default String getAuthorizationResponseCode(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getResponseCode()
                : null;
    }

    default LocalDateTime getAuthorizedAt(Transaction transaction) {
        if (transaction.getAuthorizationCode() != null) {
            LocalDateTime instant = transaction.getAuthorizationCode().getAuthorizedAt();
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        return null;
    }
}
