package com.unisono.card.limits.domain.service;

import com.unisono.card.limits.domain.model.CreditLimit;
import com.unisono.card.limits.domain.model.CreditLimitHistory;
import com.unisono.card.limits.domain.model.CreditOperation;
import com.unisono.card.limits.domain.exception.CreditOperationCancelledException;
import com.unisono.card.limits.domain.port.input.*;
import com.unisono.card.limits.domain.port.output.CardValidationPort;
import com.unisono.card.limits.domain.port.output.CreditLimitRepository;
import com.unisono.card.limits.domain.port.output.CreditOperationRepository;
import com.unisono.card.limits.domain.service.CreditLimitEventEmitter.Movement;
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
import java.util.function.Consumer;

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
    private final CreditOperationRepository creditOperationRepository;
    private final CreditLimitEventEmitter eventEmitter;

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

        // Los límites diario y mensual iniciales son parte de la asignación: un solo evento
        eventEmitter.assigned(savedLimit, command.assignedBy());

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

        // Guardar nuevo registro de historial y publicar el cambio
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
            eventEmitter.limitChanged(savedLimit, lastChange);
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

        // Guardar nuevo registro de historial y publicar el cambio
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
            eventEmitter.limitChanged(savedLimit, lastChange);
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

        // Guardar nuevo registro de historial y publicar el cambio
        CreditLimitHistory lastChange = creditLimit.getLastChange();
        if (lastChange != null && lastChange.getId() == null) {
            creditLimitRepository.saveHistory(lastChange);
            eventEmitter.limitChanged(savedLimit, lastChange);
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
        newHistory.forEach(change -> {
            creditLimitRepository.saveHistory(change);
            eventEmitter.limitChanged(savedLimit, change);
        });

        log.info("Temporal limits updated successfully");

        return savedLimit;
    }

    // ========================================
    // RecalculateAvailableCreditUseCase
    // ========================================

    @Override
    public CreditLimit consumeForPurchase(ConsumeCreditCommand command) {
        log.info("Consuming credit for purchase: {} - Amount: {}{}",
                command.creditLimitId(), command.amount(), operationLog(command.operationId()));

        return applyOperation(command.creditLimitId(), command.operationId(), CreditOperation.Type.CONSUME_PURCHASE,
                command.description(), Movement.PURCHASE, creditLimit -> creditLimit.consumeForPurchase(command.amount()));
    }

    @Override
    public CreditLimit consumeForCashAdvance(ConsumeCreditCommand command) {
        log.info("Consuming credit for cash advance: {} - Amount: {}{}",
                command.creditLimitId(), command.amount(), operationLog(command.operationId()));

        return applyOperation(command.creditLimitId(), command.operationId(), CreditOperation.Type.CONSUME_CASH_ADVANCE,
                command.description(), Movement.CASH_ADVANCE,
                creditLimit -> creditLimit.consumeForCashAdvance(command.amount()));
    }

    @Override
    public CreditLimit releaseCredit(ReleaseCreditCommand command) {
        log.info("Releasing credit: {} - Payment: {}",
                command.creditLimitId(), command.paymentAmount());

        CreditLimit creditLimit = lockCreditLimitOrThrow(command.creditLimitId());
        Money consumedBefore = creditLimit.getConsumedCredit().getAmount();

        creditLimit.releaseCredit(command.paymentAmount());

        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);
        eventEmitter.creditMoved(savedLimit, consumedBefore, Movement.PAYMENT, null);

        log.info("Credit released successfully. Available: {}",
                savedLimit.getAvailableCredit().getAmount());

        return savedLimit;
    }

    @Override
    public CreditLimit restoreCredit(RestoreCreditCommand command) {
        log.info("Restoring credit: {} - Amount: {} (original date {}) - {}{}",
                command.creditLimitId(), command.amount(), command.originalTransactionDate(), command.description(),
                operationLog(command.operationId()));

        return applyOperation(command.creditLimitId(), command.operationId(), CreditOperation.Type.RESTORE,
                command.description(), Movement.REFUND, creditLimit -> {
                    Money notRestored = creditLimit.restoreConsumption(command.amount(), command.originalTransactionDate());
                    if (!notRestored.isZero()) {
                        log.warn("Credit limit {}: {} not restored because it exceeds the consumed credit (already paid)",
                                command.creditLimitId(), notRestored);
                    }
                });
    }

    @Override
    public CancelOperationResult cancelOperation(CancelOperationCommand command) {
        log.info("Cancelling credit operation {} of credit limit {}", command.operationId(), command.creditLimitId());

        CreditLimit creditLimit = lockCreditLimitOrThrow(command.creditLimitId());
        Optional<CreditOperation> existing = creditOperationRepository.findById(command.operationId());

        if (existing.isEmpty()) {
            creditOperationRepository.save(CreditOperation.voided(
                    command.operationId(), command.creditLimitId(), creditLimit.getCurrencyCode()));
            log.info("Credit operation {} had not been applied: voided", command.operationId());
            return new CancelOperationResult(CancelOutcome.VOIDED, creditLimit);
        }

        CreditOperation operation = existing.get();
        validateOwnership(operation, command.creditLimitId());

        if (!operation.isApplied()) {
            log.info("Credit operation {} was already {}", command.operationId(), operation.getStatus());
            return new CancelOperationResult(CancelOutcome.ALREADY_CANCELLED, creditLimit);
        }

        Money consumedBefore = creditLimit.getConsumedCredit().getAmount();
        creditLimit.revertOperation(operation);
        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);
        operation.cancel();
        creditOperationRepository.save(operation);
        eventEmitter.creditMoved(savedLimit, consumedBefore, operation.getType() == CreditOperation.Type.RESTORE
                ? Movement.REFUND_CANCELLED : Movement.CONSUMPTION_CANCELLED, command.operationId());

        log.info("Credit operation {} ({}) cancelled. Available: {}",
                command.operationId(), operation.getType(), savedLimit.getAvailableCredit().getAmount());
        if (savedLimit.getOverLimitAmount().isPositive()) {
            log.warn("⚠️ Credit limit {} is over its limit by {} after undoing operation {} ({}): the restored "
                            + "credit had already been spent. New purchases are declined until it is paid",
                    command.creditLimitId(), savedLimit.getOverLimitAmount(), command.operationId(), operation.getType());
        }
        return new CancelOperationResult(CancelOutcome.CANCELLED, savedLimit);
    }

    /**
     * Aplica un consumo o una restauración con el límite bloqueado. Con operationId es idempotente:
     * una operación ya aplicada no se repite y una cancelada se rechaza.
     */
    private CreditLimit applyOperation(String creditLimitId, String operationId, CreditOperation.Type type,
                                       String description, Movement movement, Consumer<CreditLimit> action) {
        CreditLimit creditLimit = lockCreditLimitOrThrow(creditLimitId);

        if (operationId != null) {
            Optional<CreditOperation> existing = creditOperationRepository.findById(operationId);
            if (existing.isPresent()) {
                CreditOperation operation = existing.get();
                validateOwnership(operation, creditLimitId);
                if (!operation.isApplied()) {
                    throw new CreditOperationCancelledException(operationId);
                }
                if (operation.getType() != type) {
                    throw new BusinessRuleException(String.format(
                            "La operación %s ya existe con otro tipo (%s)", operationId, operation.getType()));
                }
                log.info("Credit operation {} was already applied: nothing to do", operationId);
                return creditLimit;
            }
        }

        CreditLimit.ConsumptionSnapshot before = creditLimit.consumptionSnapshot();
        action.accept(creditLimit);
        CreditLimit savedLimit = creditLimitRepository.save(creditLimit);

        if (operationId != null) {
            creditOperationRepository.save(CreditOperation.applied(
                    operationId, creditLimitId, type, before, creditLimit.consumptionSnapshot(), description));
        }
        // Solo cuando se aplicó: una operación repetida (idempotente) ya publicó su evento
        eventEmitter.creditMoved(savedLimit, before.credit(), movement, operationId);

        log.info("Credit operation {} applied. Available: {}", type, savedLimit.getAvailableCredit().getAmount());
        return savedLimit;
    }

    private static void validateOwnership(CreditOperation operation, String creditLimitId) {
        if (!operation.getCreditLimitId().equals(creditLimitId)) {
            throw new BusinessRuleException(String.format(
                    "La operación %s pertenece a otro límite de crédito", operation.getOperationId()));
        }
    }

    private static String operationLog(String operationId) {
        return operationId != null ? " (operation " + operationId + ")" : "";
    }

    private CreditLimit lockCreditLimitOrThrow(String creditLimitId) {
        return creditLimitRepository.findByIdForUpdate(creditLimitId)
                .orElseThrow(() -> new NotFoundException("CreditLimit", creditLimitId));
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
