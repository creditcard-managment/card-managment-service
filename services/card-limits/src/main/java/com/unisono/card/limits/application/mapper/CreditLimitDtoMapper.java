package com.unisono.card.limits.application.mapper;

import com.unisono.card.limits.application.dto.request.*;
import com.unisono.card.limits.application.dto.response.*;
import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.port.input.*;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper entre DTOs y Domain Models
 */
@Component
public class CreditLimitDtoMapper {

    // ========================================
    // Request → Command
    // ========================================

    public AssignCreditLimitUseCase.AssignCreditLimitCommand toCommand(AssignCreditLimitRequest request) {
        Money purchaseLimit = Money.of(request.getPurchaseLimitAmount(), request.getCurrency());
        Money dailyLimit = request.getDailyLimitAmount() != null ?
                Money.of(request.getDailyLimitAmount(), request.getCurrency()) : null;
        Money monthlyLimit = request.getMonthlyLimitAmount() != null ?
                Money.of(request.getMonthlyLimitAmount(), request.getCurrency()) : null;

        return new AssignCreditLimitUseCase.AssignCreditLimitCommand(
                CardId.of(request.getCardId()),
                purchaseLimit,
                request.getCashAdvancePercentage(),
                dailyLimit,
                monthlyLimit,
                request.getAssignedBy()
        );
    }

    public UpdateCreditLimitUseCase.IncreasePurchaseLimitCommand toIncreaseCommand(
            String creditLimitId,
            UpdateCreditLimitRequest request
    ) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new UpdateCreditLimitUseCase.IncreasePurchaseLimitCommand(
                creditLimitId,
                amount,
                request.getReason(),
                request.getChangedBy()
        );
    }

    public UpdateCreditLimitUseCase.DecreasePurchaseLimitCommand toDecreaseCommand(
            String creditLimitId,
            UpdateCreditLimitRequest request
    ) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new UpdateCreditLimitUseCase.DecreasePurchaseLimitCommand(
                creditLimitId,
                amount,
                request.getReason(),
                request.getChangedBy()
        );
    }

    public UpdateCreditLimitUseCase.UpdateCashAdvanceLimitCommand toCashAdvanceCommand(
            String creditLimitId,
            UpdateCreditLimitRequest request
    ) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new UpdateCreditLimitUseCase.UpdateCashAdvanceLimitCommand(
                creditLimitId,
                amount,
                request.getReason(),
                request.getChangedBy()
        );
    }

    public UpdateCreditLimitUseCase.UpdateTemporalLimitsCommand toTemporalCommand(
            String creditLimitId,
            UpdateTemporalLimitsRequest request
    ) {
        Money dailyLimit = request.getDailyLimitAmount() != null ?
                Money.of(request.getDailyLimitAmount(), request.getCurrency()) : null;
        Money monthlyLimit = request.getMonthlyLimitAmount() != null ?
                Money.of(request.getMonthlyLimitAmount(), request.getCurrency()) : null;

        return new UpdateCreditLimitUseCase.UpdateTemporalLimitsCommand(
                creditLimitId,
                dailyLimit,
                monthlyLimit,
                request.getReason(),
                request.getChangedBy()
        );
    }

    public RecalculateAvailableCreditUseCase.ConsumeCreditCommand toConsumeCommand(
            String creditLimitId,
            ConsumeCreditRequest request
    ) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new RecalculateAvailableCreditUseCase.ConsumeCreditCommand(
                creditLimitId,
                amount,
                request.getDescription()
        );
    }

    public RecalculateAvailableCreditUseCase.RestoreCreditCommand toRestoreCommand(
            String creditLimitId,
            RestoreCreditRequest request
    ) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new RecalculateAvailableCreditUseCase.RestoreCreditCommand(
                creditLimitId,
                amount,
                request.getOriginalTransactionDate(),
                request.getDescription()
        );
    }

    public RecalculateAvailableCreditUseCase.ReleaseCreditCommand toReleaseCommand(
            String creditLimitId,
            ReleaseCreditRequest request
    ) {
        Money amount = Money.of(request.getPaymentAmount(), request.getCurrency());
        return new RecalculateAvailableCreditUseCase.ReleaseCreditCommand(
                creditLimitId,
                amount,
                request.getDescription()
        );
    }

    public ValidatePurchaseUseCase.ValidatePurchaseCommand toValidatePurchaseCommand(
            ValidatePurchaseRequest request
    ) {
        CardId cardId = CardId.of(request.getCardId());
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new ValidatePurchaseUseCase.ValidatePurchaseCommand(cardId, amount);
    }

    public ValidatePurchaseUseCase.ValidateCashAdvanceCommand toValidateCashAdvanceCommand(
            ValidatePurchaseRequest request
    ) {
        CardId cardId = CardId.of(request.getCardId());
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        return new ValidatePurchaseUseCase.ValidateCashAdvanceCommand(cardId, amount);
    }

    // ========================================
    // Domain → Response
    // ========================================

    public CreditLimitResponse toResponse(CreditLimit creditLimit) {
        return CreditLimitResponse.builder()
                .id(creditLimit.getId())
                .cardId(creditLimit.getCardId().getValue())
                .purchaseLimit(creditLimit.getPurchaseLimit().getAmount().getAmount())
                .cashAdvanceLimit(creditLimit.getCashAdvanceLimit().getAmount().getAmount())
                .availableCredit(creditLimit.getAvailableCredit().getAmount().getAmount())
                .consumedCredit(creditLimit.getConsumedCredit().getAmount().getAmount())
                .dailyLimit(creditLimit.getDailyLimit().getAmount().getAmount())
                .dailyAvailable(creditLimit.getDailyLimit().getAvailable().getAmount())
                .monthlyLimit(creditLimit.getMonthlyLimit().getAmount().getAmount())
                .monthlyAvailable(creditLimit.getMonthlyLimit().getAvailable().getAmount())
                .currency(creditLimit.getPurchaseLimit().getAmount().getCurrencyCode())
                .createdAt(creditLimit.getCreatedAt())
                .updatedAt(creditLimit.getUpdatedAt())
                .build();
    }

    public CreditInfoResponse toCreditInfoResponse(CreditLimit creditLimit) {
        BigDecimal utilization = calculateUtilization(
                creditLimit.getConsumedCredit().getAmount().getAmount(),
                creditLimit.getPurchaseLimit().getAmount().getAmount()
        );

        return CreditInfoResponse.builder()
                .purchaseLimit(creditLimit.getPurchaseLimit().getAmount().getAmount())
                .cashAdvanceLimit(creditLimit.getCashAdvanceLimit().getAmount().getAmount())
                .availableCredit(creditLimit.getAvailableCredit().getAmount().getAmount())
                .consumedCredit(creditLimit.getConsumedCredit().getAmount().getAmount())
                .utilizationPercentage(utilization.doubleValue())
                .dailyAvailable(creditLimit.getDailyLimit().getAvailable().getAmount())
                .monthlyAvailable(creditLimit.getMonthlyLimit().getAvailable().getAmount())
                .currency(creditLimit.getPurchaseLimit().getAmount().getCurrencyCode())
                .isExhausted(creditLimit.getAvailableCredit().isExhausted())
                .build();
    }

    public ValidationResultResponse toValidationResponse(
            ValidatePurchaseUseCase.ValidationResult result,
            String currency
    ) {
        return ValidationResultResponse.builder()
                .approved(result.approved())
                .reason(result.reason())
                .availableCredit(result.availableCredit().getAmount())
                .requestedAmount(result.requestedAmount().getAmount())
                .currency(currency)
                .build();
    }

    public CreditLimitHistoryResponse toHistoryResponse(CreditLimitHistory history) {
        return CreditLimitHistoryResponse.builder()
                .id(history.getId())
                .limitType(history.getLimitType())
                .action(history.getAction())
                .previousValue(history.getPreviousValue() != null ?
                        history.getPreviousValue().getAmount() : null)
                .newValue(history.getNewValue().getAmount())
                .difference(history.getDifference().getAmount())
                .currency(history.getNewValue().getCurrencyCode())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .description(history.getDescription())
                .build();
    }

    // ========================================
    // Helper methods
    // ========================================

    private BigDecimal calculateUtilization(BigDecimal consumed, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return consumed.divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
