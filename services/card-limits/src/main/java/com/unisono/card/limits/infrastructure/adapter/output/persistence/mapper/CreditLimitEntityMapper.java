package com.unisono.card.limits.infrastructure.adapter.output.persistence.mapper;

import com.unisono.card.limits.domain.model.*;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitEntity;
import com.unisono.card.limits.infrastructure.adapter.output.persistence.entity.CreditLimitHistoryEntity;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Mapper entre CreditLimit (Domain) y CreditLimitEntity (JPA)
 */
@Component
public class CreditLimitEntityMapper {

    // ========================================
    // CreditLimit mapping
    // ========================================

    /**
     * Convierte Domain → Entity
     */
    public CreditLimitEntity toEntity(CreditLimit creditLimit) {
        if (creditLimit == null) {
            return null;
        }

        CreditLimitEntity entity = CreditLimitEntity.builder()
                .id(creditLimit.getId())
                .cardId(creditLimit.getCardId().getValue())

                // Purchase limit - ⭐ Usar getCurrencyCode()
                .purchaseLimitAmount(creditLimit.getPurchaseLimit().getAmount().getAmount())
                .purchaseLimitCurrency(creditLimit.getPurchaseLimit().getAmount().getCurrencyCode())

                // Cash advance limit - ⭐ Usar getCurrencyCode()
                .cashAdvanceLimitAmount(creditLimit.getCashAdvanceLimit().getAmount().getAmount())
                .cashAdvanceLimitCurrency(creditLimit.getCashAdvanceLimit().getAmount().getCurrencyCode())

                // Consumed credit - ⭐ Usar getCurrencyCode()
                .consumedCreditAmount(creditLimit.getConsumedCredit().getAmount().getAmount())
                .consumedCreditCurrency(creditLimit.getConsumedCredit().getAmount().getCurrencyCode())

                // Available credit - ⭐ Usar getCurrencyCode()
                .availableCreditAmount(creditLimit.getAvailableCredit().getAmount().getAmount())
                .availableCreditCurrency(creditLimit.getAvailableCredit().getAmount().getCurrencyCode())

                // Daily limit - ⭐ Usar getCurrencyCode()
                .dailyLimitAmount(creditLimit.getDailyLimit().getAmount().getAmount())
                .dailyLimitCurrency(creditLimit.getDailyLimit().getAmount().getCurrencyCode())
                .dailyLimitConsumed(creditLimit.getDailyLimit().getConsumed().getAmount())
                .dailyLimitResetDate(creditLimit.getDailyLimit().getResetDate())

                // Monthly limit - ⭐ Usar getCurrencyCode()
                .monthlyLimitAmount(creditLimit.getMonthlyLimit().getAmount().getAmount())
                .monthlyLimitCurrency(creditLimit.getMonthlyLimit().getAmount().getCurrencyCode())
                .monthlyLimitConsumed(creditLimit.getMonthlyLimit().getConsumed().getAmount())

                // Audit
                .createdAt(creditLimit.getCreatedAt())
                .updatedAt(creditLimit.getUpdatedAt())
                .build();

        // Monthly reset YearMonth
        entity.setMonthlyLimitResetYearMonth(creditLimit.getMonthlyLimit().getResetMonth());

        return entity;
    }

    /**
     * Convierte Entity → Domain
     * ⭐ CORREGIDO: Usa createMoney() helper para evitar problemas de casteo
     */
    public CreditLimit toDomain(CreditLimitEntity entity) {
        if (entity == null) {
            return null;
        }

        // Reconstruir Value Objects usando helper method
        PurchaseLimit purchaseLimit = PurchaseLimit.of(
                createMoney(entity.getPurchaseLimitAmount(), entity.getPurchaseLimitCurrency())
        );

        CashAdvanceLimit cashAdvanceLimit = CashAdvanceLimit.of(
                createMoney(entity.getCashAdvanceLimitAmount(), entity.getCashAdvanceLimitCurrency())
        );

        ConsumedCredit consumedCredit = ConsumedCredit.of(
                createMoney(entity.getConsumedCreditAmount(), entity.getConsumedCreditCurrency())
        );

        AvailableCredit availableCredit = AvailableCredit.of(
                createMoney(entity.getAvailableCreditAmount(), entity.getAvailableCreditCurrency())
        );

        DailyLimit dailyLimit = DailyLimit.reconstituteFromPersistence(
                createMoney(entity.getDailyLimitAmount(), entity.getDailyLimitCurrency()),
                createMoney(entity.getDailyLimitConsumed(), entity.getDailyLimitCurrency()),
                entity.getDailyLimitResetDate()
        );

        MonthlyLimit monthlyLimit = MonthlyLimit.reconstituteFromPersistence(
                createMoney(entity.getMonthlyLimitAmount(), entity.getMonthlyLimitCurrency()),
                createMoney(entity.getMonthlyLimitConsumed(), entity.getMonthlyLimitCurrency()),
                entity.getMonthlyLimitResetYearMonth()
        );

        return CreditLimit.reconstituteFromPersistence(
                entity.getId(),
                CardId.of(entity.getCardId()),
                purchaseLimit,
                cashAdvanceLimit,
                consumedCredit,
                availableCredit,
                dailyLimit,
                monthlyLimit,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ========================================
    // CreditLimitHistory mapping
    // ========================================

    /**
     * Convierte Domain → Entity (History)
     */
    public CreditLimitHistoryEntity toHistoryEntity(CreditLimitHistory history) {
        if (history == null) {
            return null;
        }

        return CreditLimitHistoryEntity.builder()
                .id(history.getId())
                .creditLimitId(history.getCreditLimitId())
                .limitType(history.getLimitType())
                .action(history.getAction())

                // Previous value - ⭐ CORREGIDO: Usar getCurrencyCode()
                .previousValueAmount(history.getPreviousValue() != null ?
                        history.getPreviousValue().getAmount() : null)
                .previousValueCurrency(history.getPreviousValue() != null ?
                        history.getPreviousValue().getCurrencyCode() : null)  // ← getCurrencyCode() en lugar de getCurrency()

                // New value - ⭐ CORREGIDO: Usar getCurrencyCode()
                .newValueAmount(history.getNewValue().getAmount())
                .newValueCurrency(history.getNewValue().getCurrencyCode())  // ← getCurrencyCode()

                // Difference - ⭐ CORREGIDO: Usar getCurrencyCode()
                .differenceAmount(history.getDifference().getAmount())
                .differenceCurrency(history.getDifference().getCurrencyCode())  // ← getCurrencyCode()

                // Context
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .metadata(history.getMetadata())
                .build();
    }

    /**
     * Convierte Entity → Domain (History)
     * ⭐ CORREGIDO: Usa createMoney() helper
     */
    public CreditLimitHistory toHistoryDomain(CreditLimitHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        Money previousValue = entity.getPreviousValueAmount() != null ?
                createMoney(entity.getPreviousValueAmount(), entity.getPreviousValueCurrency()) :
                null;

        Money newValue = createMoney(entity.getNewValueAmount(), entity.getNewValueCurrency());
        Money difference = createMoney(entity.getDifferenceAmount(), entity.getDifferenceCurrency());

        return CreditLimitHistory.reconstituteFromPersistence(
                entity.getId(),
                entity.getCreditLimitId(),
                entity.getLimitType(),
                entity.getAction(),
                previousValue,
                newValue,
                difference,
                entity.getReason(),
                entity.getChangedBy(),
                entity.getChangedAt(),
                entity.getMetadata()
        );
    }

    // ========================================
    // ⭐ HELPER METHOD para crear Money de forma segura
    // ========================================

    /**
     * Crea un Money a partir de BigDecimal y String de currency
     *
     * Maneja correctamente los tipos de datos para evitar errores de casteo
     */
    private Money createMoney(java.math.BigDecimal amount, String currency) {
        if (amount == null) {
            return Money.zero();
        }

        // Money.of() espera: Money.of(BigDecimal, String)
        return Money.of(amount, currency);
    }
}
