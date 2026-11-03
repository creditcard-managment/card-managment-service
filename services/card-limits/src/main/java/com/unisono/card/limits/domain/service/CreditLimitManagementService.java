package com.unisono.card.limits.domain.service;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.port.input.*;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.CardId;
import com.unisono.card.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de límites de crédito
 *
 * Implementa los use cases de:
 * - AssignCreditLimitUseCase
 * - UpdateCreditLimitUseCase
 * - GetCreditInfoUseCase
 * - RecalculateAvailableCreditUseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditLimitManagementService implements
        AssignCreditLimitUseCase,
        UpdateCreditLimitUseCase,
        GetCreditInfoUseCase,
        RecalculateAvailableCreditUseCase {

    private final CreditLimitRepository creditLimitRepository;
    private final CardValidationPort cardValidationPort;

    // ========================================
    // AssignCreditLimitUseCase
    // ========================================

    @Override
    public CreditLimit execute(AssignCreditLimitCommand command) {
        log.info("Assigning credit limit to card: {}", command.cardId());

        // Validar que la tarjeta existe y está activa
        validateCard(command.cardId());

        // Validar que no tenga límite ya asignado
        if (creditLimitRepository.existsByCardId(command.cardId())) {
            throw new BusinessRuleException(
                    "La tarjeta ya tiene un límite de crédito asignado"
            );
        }

        // Crear el límite de crédito
        CreditLimit creditLimit = CreditLimit.assign(
                command.cardId(),
                command.purchaseLimitAmount(),
                command.cashAdvancePercentage(),
                command.assignedBy()
        );

        // Actualizar límites temporales si se especifican
        if (command.dailyLimitAmount() != null) {
            creditLimit.updateDailyLimit(
                    command.dailyLimitAmount(),
                    "Límite diario inicial",
                    command.assignedBy()
            );
        }

        if (command.monthlyLimitAmount() != null) {
            creditLimit.updateMonthlyLimit(
                    command.monthlyLimitAmount(),
                    "Límite mensual inicial",
                    command.assignedBy()
            );
        }

        // Guardar
        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        // Guardar historial
        creditLimit.getHistory().forEach(creditLimitRepository::saveHistory);

        log.info("Credit limit assigned successfully: {}", savedLimit.getId());

        return savedLimit;
    }

    // ========================================
    // UpdateCreditLimitUseCase
    // ========================================

    @Override
    public CreditLimit increasePurchaseLimit(IncreasePurchaseLimitCommand command) {
        log.info("Increasing purchase limit for: {}", command.creditLimitId());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.increasePurchaseLimit(
                command.additionalAmount(),
                command.reason(),
                command.changedBy()
        );

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        // Guardar nuevo registro de historial
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
        }

        log.info("Purchase limit increased successfully");

        return savedLimit;
    }

    @Override
    public CreditLimit decreasePurchaseLimit(DecreasePurchaseLimitCommand command) {
        log.info("Decreasing purchase limit for: {}", command.creditLimitId());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.decreasePurchaseLimit(
                command.reductionAmount(),
                command.reason(),
                command.changedBy()
        );

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        // Guardar nuevo registro de historial
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
        }

        log.info("Purchase limit decreased successfully");

        return savedLimit;
    }

    @Override
    public CreditLimit updateCashAdvanceLimit(UpdateCashAdvanceLimitCommand command) {
        log.info("Updating cash advance limit for: {}", command.creditLimitId());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.updateCashAdvanceLimit(
                command.newAmount(),
                command.reason(),
                command.changedBy()
        );

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        // Guardar nuevo registro de historial
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
        }

        log.info("Cash advance limit updated successfully");

        return savedLimit;
    }

    @Override
    public CreditLimit updateTemporalLimits(UpdateTemporalLimitsCommand command) {
        log.info("Updating temporal limits for: {}", command.creditLimitId());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        if (command.dailyLimitAmount() != null) {
            creditLimit.updateDailyLimit(
                    command.dailyLimitAmount(),
                    command.reason(),
                    command.changedBy()
            );
        }

        if (command.monthlyLimitAmount() != null) {
            creditLimit.updateMonthlyLimit(
                    command.monthlyLimitAmount(),
                    command.reason(),
                    command.changedBy()
            );
        }

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        // Guardar nuevos registros de historial
        List<CreditLimitHistory> newHistory = creditLimit.getHistory().stream()
                .filter(h -> h.getId() == null)
                .toList();
        newHistory.forEach(creditLimitRepository::saveHistory);

        log.info("Temporal limits updated successfully");

        return savedLimit;
    }

    // ========================================
    // RecalculateAvailableCreditUseCase
    // ========================================

    @Override
    public CreditLimit consumeForPurchase(ConsumeCreditCommand command) {
        log.info("Consuming credit for purchase: {} - Amount: {}",
                command.creditLimitId(), command.amount());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.consumeForPurchase(command.amount());

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        log.info("Credit consumed successfully. Available: {}",
                savedLimit.getAvailableCredit().getAmount());

        return savedLimit;
    }

    @Override
    public CreditLimit consumeForCashAdvance(ConsumeCreditCommand command) {
        log.info("Consuming credit for cash advance: {} - Amount: {}",
                command.creditLimitId(), command.amount());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.consumeForCashAdvance(command.amount());

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        log.info("Credit consumed successfully. Available: {}",
                savedLimit.getAvailableCredit().getAmount());

        return savedLimit;
    }

    @Override
    public CreditLimit releaseCredit(ReleaseCreditCommand command) {
        log.info("Releasing credit: {} - Payment: {}",
                command.creditLimitId(), command.paymentAmount());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        creditLimit.releaseCredit(command.paymentAmount());

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        log.info("Credit released successfully. Available: {}",
                savedLimit.getAvailableCredit().getAmount());

        return savedLimit;
    }

    @Override
    public CreditLimit restoreCredit(RestoreCreditCommand command) {
        log.info("Restoring credit: {} - Amount: {} (original date {}) - {}",
                command.creditLimitId(), command.amount(), command.originalTransactionDate(), command.description());

        CreditLimit creditLimit = getCreditLimitOrThrow(command.creditLimitId());

        Money notRestored = creditLimit.restoreConsumption(command.amount(), command.originalTransactionDate());
        if (!notRestored.isZero()) {
            log.warn("Credit limit {}: {} not restored because it exceeds the consumed credit (already paid)",
                    command.creditLimitId(), notRestored);
        }

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        log.info("Credit restored successfully. Available: {}",
                savedLimit.getAvailableCredit().getAmount());

        return savedLimit;
    }

    // ========================================
    // GetCreditInfoUseCase
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditLimit> findById(String creditLimitId) {
        log.debug("Finding credit limit by ID: {}", creditLimitId);
        return creditLimitRepository.findById(creditLimitId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditLimit> findByCardId(CardId cardId) {
        log.debug("Finding credit limit for card: {}", cardId);
        return creditLimitRepository.findByCardId(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditLimitHistory> getHistory(String creditLimitId) {
        log.debug("Getting history for credit limit: {}", creditLimitId);

        // Verificar que el límite existe
        getCreditLimitOrThrow(creditLimitId);

        return creditLimitRepository.findHistoryByCreditLimitId(creditLimitId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLimitAssigned(CardId cardId) {
        return creditLimitRepository.existsByCardId(cardId);
    }

    // ========================================
    // Helper methods
    // ========================================

    private CreditLimit getCreditLimitOrThrow(String creditLimitId) {
        return creditLimitRepository.findById(creditLimitId)
                .orElseThrow(() -> new NotFoundException("CreditLimit", creditLimitId));
    }

    private void validateCard(CardId cardId) {
        if (!cardValidationPort.cardExists(cardId)) {
            throw new NotFoundException("Card", cardId.getValue());
        }

        if (!cardValidationPort.canAssignCreditLimit(cardId)) {
            throw new BusinessRuleException(
                    "La tarjeta no puede recibir límite de crédito en su estado actual"
            );
        }
    }
}
