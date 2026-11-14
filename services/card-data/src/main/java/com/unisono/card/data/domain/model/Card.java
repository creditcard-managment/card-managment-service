package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.exception.BusinessRuleException;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Card {

    /**
     * Intentos fallidos de PIN consecutivos que bloquean la tarjeta
     */
    public static final int MAX_PIN_ATTEMPTS = 3;

    private CardId id;
    private ClientId clientId;
    private CardHolder holder;
    private CardType type;
    private CardCategory category;
    private CardStatus status;

    private CardNumber cardNumber;
    private CVV cvv;
    // ✅ PIN en texto plano (solo transitorio, NO se persiste)
    @ToString.Exclude
    private PIN pin;

    // ✅ Hash del PIN (esto SÍ se persiste en BD)
    private String pinHash;

    // Intentos fallidos de PIN consecutivos (se reinicia con un PIN correcto)
    @Builder.Default
    private int pinFailedAttempts = 0;
    private ExpirationDate expirationDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime blockedAt;
    private LocalDateTime issuedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime expiredAt;

    private String blockReason;
    private String cancelledReason;

    @Builder.Default
    private Boolean deleted = false;
    private LocalDateTime deletedAt;

    /**
     * Versión de la fila en la base (control de concurrencia optimista). Nula hasta que la tarjeta se guarda por
     * primera vez; la asigna el repositorio con {@link #syncVersion(Long)}.
     */
    private Long version;

    // =============================================
    // FACTORY METHOD
    // =============================================

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
                .deleted(false)
                .build();
    }

    /**
     * Sobrecarga con más campos (para compatibilidad futura)
     */
    public static Card reconstituteFromPersistence(
            CardId id,
            ClientId clientId,
            CardHolder holder,
            CardNumber cardNumber,
            CVV cvv,
            String pinHash,
            int pinFailedAttempts,
            CardType type,
            CardCategory category,
            ExpirationDate expirationDate,
            CardStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime activatedAt,
            LocalDateTime blockedAt,
            LocalDateTime issuedAt,
            LocalDateTime cancelledAt,
            LocalDateTime expiredAt,
            String blockReason,
            String cancelledReason,
            Boolean deleted,
            LocalDateTime deletedAt,
            Long version
    ) {
        return Card.builder()
                .id(id)
                .clientId(clientId)
                .holder(holder)
                .cardNumber(cardNumber)
                .cvv(cvv)
                .pinHash(pinHash)
                .pinFailedAttempts(pinFailedAttempts)
                .pin(null)
                .type(type)
                .category(category)
                .expirationDate(expirationDate)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .activatedAt(activatedAt)
                .blockedAt(blockedAt)
                .issuedAt(issuedAt)
                .cancelledAt(cancelledAt)
                .expiredAt(expiredAt)
                .blockReason(blockReason)
                .cancelledReason(cancelledReason)
                .deleted(deleted != null ? deleted : false)
                .deletedAt(deletedAt)
                .version(version)
                .build();
    }

    /**
     * Actualiza la versión después de guardar, para que un segundo guardado de la misma instancia en la misma
     * operación no se tome como un cambio concurrente. Solo la usa el repositorio.
     */
    public void syncVersion(Long version) {
        this.version = version;
    }

    private static void validateCreationParameters(
            ClientId clientId,
            CardHolder holder,
            CardType type,
            CardCategory category
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException("ClientId no puede ser null");
        }
        if (holder == null) {
            throw new IllegalArgumentException("CardHolder no puede ser null");
        }
        if (type == null) {
            throw new IllegalArgumentException("CardType no puede ser null");
        }
        if (category == null) {
            throw new IllegalArgumentException("CardCategory no puede ser null");
        }
    }

    // =============================================
    // MÉTODOS DE ACTUALIZACIÓN
    // =============================================

    public void updateStatus(CardStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser null");
        }
        validateTransitionTo(newStatus);
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        if (newStatus == CardStatus.EXPIRED && this.expiredAt == null) {
            // Igual que expire(): el vencimiento por PUT /status también registra cuándo ocurrió
            this.expiredAt = this.updatedAt;
        }
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCardData(
            CardNumber cardNumber,
            CVV cvv,
            PIN pin,
            ExpirationDate expirationDate
    ) {
        if (cardNumber == null) throw new IllegalArgumentException("CardNumber no puede ser null");
        if (cvv == null) throw new IllegalArgumentException("CVV no puede ser null");
        if (pin == null) throw new IllegalArgumentException("PIN no puede ser null");
        if (expirationDate == null) throw new IllegalArgumentException("ExpirationDate no puede ser null");

        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.pin = pin;
        this.expirationDate = expirationDate;
        this.issuedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void block(String reason) {
        validateCanBlock();
        this.status = CardStatus.BLOCKED;
        this.blockReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void blockForFraud(String reason) {
        validateCanBlock();
        this.status = CardStatus.BLOCKED_FRAUD;
        this.blockReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void unblock() {
        if (!this.status.isBlocked()) {
            throw new BusinessRuleException(
                    String.format("La tarjeta no está bloqueada. Estado actual: %s", this.status)
            );
        }
        this.status = CardStatus.ACTIVE;
        this.blockReason = null;
        this.blockedAt = null;
        this.pinFailedAttempts = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        validateTransitionTo(CardStatus.CANCELLED);
        this.status = CardStatus.CANCELLED;
        this.cancelledReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void expire() {
        validateTransitionTo(CardStatus.EXPIRED);
        this.status = CardStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void validateTransitionTo(CardStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    String.format("Transición de estado inválida: %s → %s", this.status, target)
            );
        }
    }

    private void validateCanBlock() {
        if (!this.status.canBlock()) {
            throw new BusinessRuleException(
                    String.format("No se puede bloquear una tarjeta en estado %s", this.status)
            );
        }
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // =============================================
    // MÉTODOS DE VALIDACIÓN
    // =============================================

    public boolean isActive() {
        return CardStatus.ACTIVE.equals(this.status);
    }

    public boolean isBlocked() {
        return this.status.isBlocked(); // ✅ Delegar al enum
    }

    public boolean isOperational() {
        // Una tarjeta está operativa si el estado es operativo Y no está expirada
        return this.status.isOperational() && !isExpired();
    }

    public boolean isExpired() {
        if (this.expirationDate == null) {
            return false;
        }
        return this.expirationDate.isExpired();
    }

    public boolean requiresRenewal() {
        if (this.expirationDate == null) {
            return false;
        }

        // Considerar que requiere renovación 60 días antes de expirar
        LocalDate expirationLocalDate = LocalDate.of(
                expirationDate.getYear(),
                expirationDate.getMonth(),
                1
        ).plusMonths(1).minusDays(1); // Último día del mes de expiración

        LocalDate renewalThreshold = LocalDate.now().plusDays(60);

        return expirationLocalDate.isBefore(renewalThreshold);
    }

    public boolean canBeActivated() {
        return this.status.canActivate(); // ✅ Delegar al enum
    }

    public boolean canBeIssued() {
        return CardStatus.APPROVED.equals(this.status);
    }

    // =============================================
    // MÉTODOS PARA GESTIÓN DE PIN
    // =============================================

    /**
     * Obtiene el hash del PIN almacenado
     */
    public String getPinHash() {
        return this.pinHash;
    }

    /**
     * Actualiza el PIN con un nuevo hash
     */
    public void updatePIN(String hashedPin) {
        if (hashedPin == null || hashedPin.isBlank()) {
            throw new IllegalArgumentException("El PIN hasheado no puede estar vacío");
        }
        this.pinHash = hashedPin;
        this.pin = null; // Limpiar PIN plano
        this.pinFailedAttempts = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Resetea el PIN (usado en flujo de recuperación)
     */
    public void resetPIN(String hashedPin) {
        if (hashedPin == null || hashedPin.isBlank()) {
            throw new IllegalArgumentException("El PIN hasheado no puede estar vacío");
        }
        this.pinHash = hashedPin;
        this.pin = null; // Limpiar PIN plano
        this.pinFailedAttempts = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Registra un intento fallido de PIN.
     * Al alcanzar MAX_PIN_ATTEMPTS bloquea la tarjeta (si su estado lo permite).
     *
     * @return true si la tarjeta quedó bloqueada por este intento
     */
    public boolean registerFailedPinAttempt() {
        this.pinFailedAttempts++;
        this.updatedAt = LocalDateTime.now();

        if (this.pinFailedAttempts >= MAX_PIN_ATTEMPTS && this.status.canBlock()) {
            block(String.format("Bloqueo automático: %d intentos fallidos de PIN", this.pinFailedAttempts));
            return true;
        }
        return false;
    }

    /**
     * Reinicia el contador de intentos fallidos (tras un PIN correcto)
     */
    public void resetPinFailedAttempts() {
        if (this.pinFailedAttempts != 0) {
            this.pinFailedAttempts = 0;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Intentos de PIN restantes antes del bloqueo automático
     */
    public int getRemainingPinAttempts() {
        return Math.max(0, MAX_PIN_ATTEMPTS - this.pinFailedAttempts);
    }

    /**
     * Establece el PIN plano (solo en memoria, para hashear después)
     */
    public void setPINPlainText(PIN pin) {
        this.pin = pin;
    }

    /**
     * Obtiene el PIN plano (solo si existe en memoria)
     */
    public PIN getPINPlainText() {
        return this.pin;
    }


    // =============================================
    // MÉTODOS DE UTILIDAD
    // =============================================

    /**
     * Verifica si la tarjeta tiene un PIN configurado
     */
    public boolean isPINSet() {
        return this.pinHash != null && !this.pinHash.isBlank();
    }

    public String getMaskedCardNumber() {
        if (this.cardNumber == null) {
            return null;
        }

        String lastFour = this.cardNumber.getLastFourDigits();
        return "**** **** **** " + lastFour;
    }

    public boolean hasCompleteCardData() {
        return this.cardNumber != null
                && this.cvv != null
                && isPINSet()  // ✅ Verificar hash
                && this.expirationDate != null;
    }

}
