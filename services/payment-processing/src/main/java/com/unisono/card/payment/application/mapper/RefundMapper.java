package com.unisono.card.payment.application.mapper;

import com.unisono.card.payment.application.dto.request.ProcessRefundCommand;
import com.unisono.card.payment.application.dto.response.RefundResponse;
import com.unisono.card.payment.domain.model.*;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.mapstruct.*;

import java.util.UUID;

/**
 * Mapper for converting between Refund commands/responses and domain models
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RefundMapper {

    /**
     * Convert ProcessRefundCommand to domain objects
     */
    default RefundComponents toRefundComponents(ProcessRefundCommand command) {
        CardId cardId = CardId.of(command.getCardId()); // ✅ String → CardId
        TransactionId originalTransactionId = TransactionId.of(command.getOriginalTransactionId());
        Money refundAmount = Money.of(command.getRefundAmount(), command.getCurrency());
        TransactionAmount amount = TransactionAmount.of(refundAmount);

        return RefundComponents.builder()
                .cardId(cardId)
                .originalTransactionId(originalTransactionId)
                .amount(amount)
                .reason(command.getReason())
                .isPartialRefund(command.isPartialRefund())
                .merchantId(command.getMerchantId())
                .build();
    }

    /**
     * Convert Transaction (refund) to RefundResponse
     */
    @Mapping(target = "refundTransactionId", expression = "java(transaction.getTransactionId().toString())")
    @Mapping(target = "originalTransactionId", expression = "java(transaction.getOriginalTransactionId() != null ? transaction.getOriginalTransactionId().toString() : null)")
    @Mapping(target = "cardId", expression = "java(transaction.getCardId().toString())")
    @Mapping(target = "refundAmount", expression = "java(transaction.getAmount().getAmount().getAmount())")
    @Mapping(target = "currency", expression = "java(transaction.getAmount().getAmount().getCurrency().toString())")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "processedAt", source = "transactionDate")
    @Mapping(target = "approved", expression = "java(transaction.isSuccessful())")
    @Mapping(target = "responseMessage", expression = "java(getResponseMessage(transaction))")
    RefundResponse toRefundResponse(Transaction transaction);

    default String getResponseMessage(Transaction transaction) {
        if (transaction.isSuccessful()) {
            return "Refund processed successfully";
        } else if (transaction.getStatus() == TransactionStatus.DECLINED) {
            return "Refund declined: " + transaction.getDeclineReason();
        }
        return "Refund " + transaction.getStatus().getDisplayName();
    }

    /**
     * Helper class to hold refund components
     */
    @lombok.Builder
    @lombok.Data
    class RefundComponents {
        private CardId cardId;
        private TransactionId originalTransactionId;
        private TransactionAmount amount;
        private String reason;
        private boolean isPartialRefund;
        private String merchantId;
    }
}
