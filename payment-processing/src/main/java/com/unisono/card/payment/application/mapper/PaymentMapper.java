package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.request.ProcessPaymentCommand;
import com.unisono.card.payment.application.dto.response.PaymentResponse;
import com.unisono.card.payment.domain.model.*;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.ExpirationDate;
import com.unisono.card.shared.domain.valueobject.Money;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Mapper for converting between Payment commands/responses and domain models
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentMapper {

    /**
     * Convert ProcessPaymentCommand to domain objects for creating a transaction
     */
    @Mapping(target = "cardId", expression = "java(mapToCardId(command.getCardId()))")
    @Mapping(target = "cardData", expression = "java(mapToCardData(command))")
    @Mapping(target = "merchantInfo", expression = "java(mapToMerchantInfo(command))")
    @Mapping(target = "amount", expression = "java(mapToTransactionAmount(command))")
    default TransactionComponents toTransactionComponents(ProcessPaymentCommand command) {
        return TransactionComponents.builder()
                .cardId(mapToCardId(command.getCardId())) // ✅ String → CardId
                .cardData(mapToCardData(command))
                .merchantInfo(mapToMerchantInfo(command))
                .amount(mapToTransactionAmount(command))
                .paymentMethod(command.getPaymentMethod())
                .build();
    }

    /**
     * Convert Transaction domain model to PaymentResponse DTO
     */
    @Mapping(target = "transactionId", expression = "java(transaction.getTransactionId().toString())")
    @Mapping(target = "cardId", expression = "java(transaction.getCardId().toString())")
    @Mapping(target = "transactionType", source = "type")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "amount", expression = "java(transaction.getAmount().getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(transaction.getAmount().getAmount().getCurrency().toString())")
    @Mapping(target = "authorizationCode", expression = "java(getAuthorizationCodeValue(transaction))")
    @Mapping(target = "responseCode", expression = "java(getResponseCode(transaction))")
    @Mapping(target = "responseMessage", expression = "java(getResponseMessage(transaction))")
    @Mapping(target = "riskScore", source = "riskScore")
    @Mapping(target = "merchantId", expression = "java(transaction.getMerchantInfo().getMerchantId())")
    @Mapping(target = "merchantName", expression = "java(transaction.getMerchantInfo().getMerchantName())")
    @Mapping(target = "transactionDate", source = "transactionDate")
    @Mapping(target = "authorizedAt", expression = "java(getAuthorizedAt(transaction))")
    @Mapping(target = "approved", expression = "java(transaction.isSuccessful())")
    @Mapping(target = "declineReason", source = "declineReason")
    PaymentResponse toPaymentResponse(Transaction transaction);

    // ==================== Helper mapping methods ====================

    default CardId mapToCardId(String cardIdString) {
        return CardId.of(cardIdString); // ✅ String → CardId
    }

    default CardData mapToCardData(ProcessPaymentCommand command) {
        ExpirationDate expirationDate = ExpirationDate.of(command.getExpirationDate());

        return CardData.of(
                command.getCardNumberToken(),
                command.getLastFourDigits(),
                command.getCardholderName(),
                expirationDate
        );
    }

    default MerchantInfo mapToMerchantInfo(ProcessPaymentCommand command) {
        return MerchantInfo.of(
                command.getMerchantId(),
                command.getMerchantName(),
                command.getMerchantCategoryCode(),
                command.getMerchantCity(),
                command.getMerchantCountry()
        );
    }

    default TransactionAmount mapToTransactionAmount(ProcessPaymentCommand command) {
        Money amount = Money.of(command.getAmount(), command.getCurrency());

        // Check if there's currency conversion
        if (command.getLocalAmount() != null && command.getLocalCurrency() != null
                && command.getExchangeRate() != null) {
            Money localAmount = Money.of(command.getLocalAmount(), command.getLocalCurrency());
            return TransactionAmount.withConversion(amount, localAmount, command.getExchangeRate());
        }

        return TransactionAmount.of(amount);
    }

    default String getAuthorizationCodeValue(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getCode()
                : null;
    }

    default String getResponseCode(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getResponseCode()
                : null;
    }

    default String getResponseMessage(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            return "Transaction approved successfully";
        } else if (transaction.getStatus() == TransactionStatus.DECLINED) {
            return "Transaction declined: " + transaction.getDeclineReason();
        } else if (transaction.getStatus() == TransactionStatus.REVERSED) {
            return "Transaction reversed";
        }
        return transaction.getStatus().getDisplayName();
    }

    default java.time.LocalDateTime getAuthorizedAt(Transaction transaction) {
        return transaction.getAuthorizationCode() != null
                ? transaction.getAuthorizationCode().getAuthorizedAt()  // ✅ Retorna LocalDateTime directamente
                : null;
    }

    /**
     * Helper class to hold transaction components
     */
    @lombok.Builder
    @lombok.Data
    class TransactionComponents {
        private CardId cardId;
        private CardData cardData;
        private MerchantInfo merchantInfo;
        private TransactionAmount amount;
        private PaymentMethod paymentMethod;
    }
}
