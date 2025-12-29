package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.valueobject.*;
import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate Root: Card
 *
 * Representa una tarjeta de crédito en su totalidad.
 * Contiene toda la lógica de negocio relacionada con:
 * - Creación y emisión de tarjetas
 * - Bloqueo/desbloqueo
 * - Validación de estado
 * - Gestión de datos sensibles (PAN, CVV, PIN)
 *
 * Invariantes:
 * - Una tarjeta bloqueada no puede realizar operaciones
 * - Una tarjeta vencida debe ser renovada
 * - El PIN solo puede ser reseteado si la tarjeta está activa
 * - Una tarjeta cancelada no puede volver a activarse
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Card {

    private CardId id;
    private ClientId clientId;
    private CardHolder holder;

    // Datos sensibles (siempre encriptados en BD)
    private CardNumber cardNumber;
    private CVV cvv;
    private String pinHash; // BCrypt hash del PIN

    // Metadatos de la tarjeta
    private CardType type;
    private CardCategory category;
    private ExpirationDate expirationDate;
    private CardStatus status;

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime blockedAt;
    private String blockReason;

    // ========================================
    // FACTORY METHODS
    // ========================================

    /**
     * Crea una nueva tarjeta (estado REQUESTED)
     * Este es el primer paso en el ciclo de vida.
     */
    public static Card create(
            ClientId clientId,
            CardHolder holder,
            CardType type,
            CardCategory category
    ) {
        validateCreationParameters(clientId, holder, type, category);

        LocalDateTime now = LocalDateTime.now();

        return Card.builder()
                .id(CardId.generate())
                .clientId(clientId)
                .holder(holder)
                .type(type)
                .category(category)
                .status(CardStatus.REQUESTED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Card reconstituteFromPersistence(
            CardId id,
            ClientId clientId,
            CardHolder holder,
            CardNumber cardNumber,
            CVV cvv,
            String pinHash,
            CardType type,
            CardCategory category,
            ExpirationDate expirationDate,
            CardStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime activatedAt,
            LocalDateTime blockedAt,
            String blockReason
    ) {
        return Card.builder()
                .id(id)
                .clientId(clientId)
                .holder(holder)
                .cardNumber(cardNumber)
                .cvv(cvv)
                .pinHash(pinHash)
                .type(type)
                .category(category)
                .expirationDate(expirationDate)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .activatedAt(activatedAt)
                .blockedAt(blockedAt)
                .blockReason(blockReason)
                .build();
    }

    /**
     * Emite la tarjeta física (asigna número, CVV, genera PIN)
     * Solo puede ser llamado después de que la tarjeta sea aprobada.
     */
    public void issue(
            CardNumber cardNumber,
            CVV cvv,
            String pinHash,
            ExpirationDate expirationDate
    ) {
        validateCanBeIssued();
        validateIssuanceParameters(cardNumber, cvv, pinHash, expirationDate);

        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.pinHash = pinHash;
        this.expirationDate = expirationDate;
        this.status = CardStatus.ISSUED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // BUSINESS LOGIC - LIFECYCLE
    // ========================================

    /**
     * Activa la tarjeta (el cliente la recibió y la activó)
     */
    public void activate() {
        if (status == CardStatus.ACTIVE) {
            throw new BusinessRuleException("La tarjeta ya está activa");
        }

        if (status != CardStatus.ISSUED) {
            throw new BusinessRuleException(
                    "Solo se pueden activar tarjetas en estado ISSUED. Estado actual: " + status
            );
        }

        if (expirationDate.isExpired()) {
            throw new BusinessRuleException("No se puede activar una tarjeta vencida");
        }

        this.status = CardStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Bloquea la tarjeta (por solicitud del cliente o por seguridad)
     */
    public void block(String reason) {
        if (status == CardStatus.BLOCKED || status == CardStatus.BLOCKED_FRAUD) {
            throw new BusinessRuleException("La tarjeta ya está bloqueada");
        }

        if (status == CardStatus.CANCELLED) {
            throw new BusinessRuleException("No se puede bloquear una tarjeta cancelada");
        }

        if (status != CardStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Solo se pueden bloquear tarjetas activas. Estado actual: " + status
            );
        }

        this.status = CardStatus.BLOCKED;
        this.blockReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Bloquea la tarjeta por sospecha de fraude
     */
    public void blockForFraud(String reason) {
        if (status == CardStatus.CANCELLED) {
            throw new BusinessRuleException("No se puede bloquear una tarjeta cancelada");
        }

        this.status = CardStatus.BLOCKED_FRAUD;
        this.blockReason = "FRAUDE: " + reason;
        this.blockedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Desbloquea la tarjeta
     */
    public void unblock() {
        if (status != CardStatus.BLOCKED) {
            throw new BusinessRuleException(
                    "Solo se pueden desbloquear tarjetas en estado BLOCKED. Estado actual: " + status
            );
        }

        if (expirationDate.isExpired()) {
            throw new BusinessRuleException("No se puede desbloquear una tarjeta vencida");
        }

        this.status = CardStatus.ACTIVE;
        this.blockReason = null;
        this.blockedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancela la tarjeta definitivamente (no reversible)
     */
    public void cancel(String reason) {
        if (status == CardStatus.CANCELLED) {
            throw new BusinessRuleException("La tarjeta ya está cancelada");
        }

        this.status = CardStatus.CANCELLED;
        this.blockReason = "CANCELADA: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca la tarjeta como vencida (proceso automático)
     */
    public void markAsExpired() {
        if (!expirationDate.isExpired()) {
            throw new BusinessRuleException("La tarjeta aún no está vencida");
        }

        if (status == CardStatus.CANCELLED) {
            return; // Ya está en estado final
        }

        this.status = CardStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // BUSINESS LOGIC - PIN MANAGEMENT
    // ========================================

    /**
     * Valida si el PIN proporcionado es correcto
     * (la comparación real se hace en el servicio usando BCrypt)
     */
    public boolean isPINSet() {
        return pinHash != null && !pinHash.isEmpty();
    }

    /**
     * Resetea el PIN (requiere que la tarjeta esté activa)
     */
    public void resetPIN(String newPinHash) {
        if (status != CardStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Solo se puede resetear el PIN de tarjetas activas. Estado actual: " + status
            );
        }

        if (newPinHash == null || newPinHash.isEmpty()) {
            throw new ValidationException("El hash del nuevo PIN no puede ser nulo");
        }

        this.pinHash = newPinHash;
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // BUSINESS LOGIC - VALIDATIONS
    // ========================================

    /**
     * Verifica si la tarjeta puede realizar operaciones
     */
    public boolean isOperational() {
        return status == CardStatus.ACTIVE && !expirationDate.isExpired();
    }

    /**
     * Verifica si la tarjeta está bloqueada
     */
    public boolean isBlocked() {
        return status == CardStatus.BLOCKED || status == CardStatus.BLOCKED_FRAUD;
    }

    /**
     * Verifica si la tarjeta está próxima a vencer (dentro de 30 días)
     */
    public boolean isNearExpiration() {
        return expirationDate.expiresWithinMonths(1);
    }

    /**
     * Verifica si la tarjeta requiere renovación
     */
    public boolean requiresRenewal() {
        return isNearExpiration() && status == CardStatus.ACTIVE;
    }

    // ========================================
    // PRIVATE VALIDATIONS
    // ========================================

    private static void validateCreationParameters(
            ClientId clientId,
            CardHolder holder,
            CardType type,
            CardCategory category
    ) {
        if (clientId == null) {
            throw new ValidationException("El ID del cliente es obligatorio");
        }
        if (holder == null) {
            throw new ValidationException("El titular de la tarjeta es obligatorio");
        }
        if (type == null) {
            throw new ValidationException("El tipo de tarjeta es obligatorio");
        }
        if (category == null) {
            throw new ValidationException("La categoría de tarjeta es obligatoria");
        }
    }

    private void validateCanBeIssued() {
        if (status != CardStatus.APPROVED && status != CardStatus.REQUESTED) {
            throw new BusinessRuleException(
                    "Solo se pueden emitir tarjetas aprobadas o solicitadas. Estado actual: " + status
            );
        }

        if (cardNumber != null) {
            throw new BusinessRuleException("La tarjeta ya fue emitida previamente");
        }
    }

    private void validateIssuanceParameters(
            CardNumber cardNumber,
            CVV cvv,
            String pinHash,
            ExpirationDate expirationDate
    ) {
        if (cardNumber == null) {
            throw new ValidationException("El número de tarjeta es obligatorio");
        }
        if (cvv == null) {
            throw new ValidationException("El CVV es obligatorio");
        }
        if (pinHash == null || pinHash.isEmpty()) {
            throw new ValidationException("El PIN hash es obligatorio");
        }
        if (expirationDate == null) {
            throw new ValidationException("La fecha de vencimiento es obligatoria");
        }
        if (expirationDate.isExpired()) {
            throw new ValidationException("No se puede emitir una tarjeta con fecha vencida");
        }
    }

    // ========================================
    // BUSINESS LOGIC - STATE TRANSITIONS
    // ========================================

    /**
     * Aprueba la tarjeta (después de validación crediticia)
     */
    public void approve() {
        if (status != CardStatus.REQUESTED) {
            throw new BusinessRuleException(
                    "Solo se pueden aprobar tarjetas solicitadas. Estado actual: " + status
            );
        }

        this.status = CardStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rechaza la tarjeta (después de validación crediticia)
     */
    public void reject(String reason) {
        if (status != CardStatus.REQUESTED) {
            throw new BusinessRuleException(
                    "Solo se pueden rechazar tarjetas solicitadas. Estado actual: " + status
            );
        }

        this.status = CardStatus.CANCELLED;
        this.blockReason = "RECHAZADA: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Actualiza el timestamp de última modificación
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el número de tarjeta enmascarado (para mostrar al usuario)
     */
    public String getMaskedCardNumber() {
        return cardNumber != null ? cardNumber.getMasked() : "****";
    }

    // ========================================
    // EQUALS & HASHCODE
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Card{id=%s, clientId=%s, type=%s, category=%s, status=%s, masked=%s}",
                id, clientId, type, category, status, getMaskedCardNumber()
        );
    }
}
