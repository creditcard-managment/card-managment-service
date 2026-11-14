package com.unisono.card.limits.domain.service;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.port.input.ValidatePurchaseUseCase;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de validación de transacciones contra límites
 * Implementa: ValidatePurchaseUseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditValidationService implements ValidatePurchaseUseCase {

    static final String CARD_NOT_ACTIVE = "La tarjeta no está activa";

    private final CreditLimitRepository creditLimitRepository;
    private final CardValidationPort cardValidationPort;

    @Override
    public ValidationResult validatePurchase(ValidatePurchaseCommand command) {
        log.debug("Validating purchase: {} - Amount: {}",
                command.cardId(), command.purchaseAmount());

        CreditLimit creditLimit = creditLimitRepository.findByCardId(command.cardId())
                .orElseThrow(() -> new NotFoundException(
                        "CreditLimit",
                        "for card " + command.cardId().getValue()
                ));

        Money availableCredit = creditLimit.getAvailableCredit().getAmount();

        if (!cardValidationPort.isCardActive(command.cardId())) {
            return ValidationResult.rejected(CARD_NOT_ACTIVE, availableCredit, command.purchaseAmount());
        }

        // Validar si la compra puede realizarse
        if (!creditLimit.canPurchase(command.purchaseAmount())) {
            String reason = determineRejectionReason(creditLimit, command.purchaseAmount(), false);
            return ValidationResult.rejected(reason, availableCredit, command.purchaseAmount());
        }

        return ValidationResult.approved(availableCredit, command.purchaseAmount());
    }

    @Override
    public ValidationResult validateCashAdvance(ValidateCashAdvanceCommand command) {
        log.debug("Validating cash advance: {} - Amount: {}",
                command.cardId(), command.advanceAmount());

        CreditLimit creditLimit = creditLimitRepository.findByCardId(command.cardId())
                .orElseThrow(() -> new NotFoundException(
                        "CreditLimit",
                        "for card " + command.cardId().getValue()
                ));

        Money availableCredit = creditLimit.getAvailableCredit().getAmount();

        if (!cardValidationPort.isCardActive(command.cardId())) {
            return ValidationResult.rejected(CARD_NOT_ACTIVE, availableCredit, command.advanceAmount());
        }

        // Validar si el adelanto puede realizarse
        if (!creditLimit.canCashAdvance(command.advanceAmount())) {
            String reason = determineRejectionReason(creditLimit, command.advanceAmount(), true);
            return ValidationResult.rejected(reason, availableCredit, command.advanceAmount());
        }

        return ValidationResult.approved(availableCredit, command.advanceAmount());
    }

    // ========================================
    // Helper methods
    // ========================================

    private String determineRejectionReason(
            CreditLimit creditLimit,
            Money requestedAmount,
            boolean isCashAdvance
    ) {
        // Verificar límite principal
        if (isCashAdvance) {
            if (!creditLimit.getCashAdvanceLimit().allows(requestedAmount)) {
                return String.format(
                        "Se excede el límite de adelantos de %s",
                        creditLimit.getCashAdvanceLimit().getAmount()
                );
            }
        } else {
            if (!creditLimit.getPurchaseLimit().allows(requestedAmount)) {
                return String.format(
                        "Se excede el límite de compras de %s",
                        creditLimit.getPurchaseLimit().getAmount()
                );
            }
        }

        // Verificar crédito disponible
        if (!creditLimit.getAvailableCredit().isSufficientFor(requestedAmount)) {
            return String.format(
                    "Crédito insuficiente. Disponible: %s",
                    creditLimit.getAvailableCredit().getAmount()
            );
        }

        // Verificar límite diario
        if (!creditLimit.getDailyLimit().allows(requestedAmount)) {
            return String.format(
                    "Se excede el límite diario. Disponible hoy: %s",
                    creditLimit.getDailyLimit().getAvailable()
            );
        }

        // Verificar límite mensual
        if (!creditLimit.getMonthlyLimit().allows(requestedAmount)) {
            return String.format(
                    "Se excede el límite mensual. Disponible este mes: %s",
                    creditLimit.getMonthlyLimit().getAvailable()
            );
        }

        return "La transacción no puede ser procesada";
    }
}
