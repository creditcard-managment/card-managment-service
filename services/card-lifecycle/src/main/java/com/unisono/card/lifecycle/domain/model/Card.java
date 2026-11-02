package com.unisono.card.lifecycle.domain.model;

import com.unisono.card.shared.domain.valueobject.*;
import lombok.*;

/**
 * Modelo de dominio Card para el módulo Lifecycle
 * Solo contiene los atributos necesarios para gestionar el ciclo de vida
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    private CardId id;
    private CardStatus status;
    private String clientId;

    // ✅ NUEVOS: Datos de emisión (se asignan cuando se emite la tarjeta)
    private CardNumber cardNumber;
    private CVV cvv;
    private String pinHash; // PIN ya encriptado
    private ExpirationDate expirationDate;

    // =============================================
    // MÉTODOS DE NEGOCIO - Transiciones de Estado
    // =============================================

    /**
     * Aprueba la tarjeta (REQUESTED → APPROVED)
     */
    public void approve() {
        validateTransition(CardStatus.REQUESTED, CardStatus.APPROVED);
        this.status = CardStatus.APPROVED;
    }

    /**
     * Rechaza la tarjeta (REQUESTED → REJECTED)
     */
    public void reject() {
        validateTransition(CardStatus.REQUESTED, CardStatus.REJECTED);
        this.status = CardStatus.REJECTED;
    }

    /**
     * Emite la tarjeta (APPROVED → ISSUED)
     * Asigna número de tarjeta, CVV, PIN y fecha de expiración
     *
     * @param cardNumber Número de la tarjeta
     * @param cvv CVV de la tarjeta
     * @param pinHash PIN ya encriptado
     * @param expirationDate Fecha de expiración
     */
    public void issue(CardNumber cardNumber, CVV cvv, String pinHash, ExpirationDate expirationDate) {
        validateTransition(CardStatus.APPROVED, CardStatus.ISSUED);

        // Validar parámetros
        if (cardNumber == null) {
            throw new IllegalArgumentException("Número de tarjeta no puede ser null");
        }
        if (cvv == null) {
            throw new IllegalArgumentException("CVV no puede ser null");
        }
        if (pinHash == null || pinHash.isBlank()) {
            throw new IllegalArgumentException("PIN hash no puede ser null o vacío");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("Fecha de expiración no puede ser null");
        }
        if (expirationDate.isExpired()) {
            throw new IllegalArgumentException("Fecha de expiración ya está vencida");
        }

        // Asignar datos de emisión
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.pinHash = pinHash;
        this.expirationDate = expirationDate;

        // Cambiar estado
        this.status = CardStatus.ISSUED;
    }

    /**
     * Activa la tarjeta (ISSUED → ACTIVE)
     */
    public void activate() {
        validateTransition(CardStatus.ISSUED, CardStatus.ACTIVE);
        this.status = CardStatus.ACTIVE;
    }

    /**
     * Bloquea la tarjeta (ACTIVE → BLOCKED)
     */
    public void block() {
        validateTransition(CardStatus.ACTIVE, CardStatus.BLOCKED);
        this.status = CardStatus.BLOCKED;
    }

    /**
     * Bloquea la tarjeta por fraude (ACTIVE → BLOCKED_FRAUD)
     */
    public void blockForFraud() {
        validateTransition(CardStatus.ACTIVE, CardStatus.BLOCKED_FRAUD);
        this.status = CardStatus.BLOCKED_FRAUD;
    }

    /**
     * Desbloquea la tarjeta (BLOCKED → ACTIVE)
     */
    public void unblock() {
        if (status != CardStatus.BLOCKED) {
            throw new IllegalStateException(
                    String.format("Solo se pueden desbloquear tarjetas en estado BLOCKED. Estado actual: %s", status)
            );
        }
        this.status = CardStatus.ACTIVE;
    }

    /**
     * Cancela la tarjeta (ACTIVE/BLOCKED → CANCELLED)
     */
    public void cancel() {
        if (status == CardStatus.CANCELLED || status == CardStatus.REJECTED || status == CardStatus.EXPIRED) {
            throw new IllegalStateException(
                    String.format("No se puede cancelar una tarjeta en estado final: %s", status)
            );
        }
        this.status = CardStatus.CANCELLED;
    }

    /**
     * Valida que la transición de estado sea permitida
     */
    private void validateTransition(CardStatus expectedFrom, CardStatus to) {
        if (this.status != expectedFrom) {
            throw new IllegalStateException(
                    String.format("Transición inválida: %s → %s. Estado actual: %s",
                            expectedFrom, to, this.status)
            );
        }
    }

    // =============================================
    // MÉTODOS DE CONSULTA
    // =============================================

    /**
     * Verifica si la tarjeta está en un estado final
     */
    public boolean isFinalState() {
        return status.isFinal();
    }

    /**
     * Verifica si la tarjeta puede ser activada
     */
    public boolean canActivate() {
        return status == CardStatus.ISSUED;
    }

    /**
     * Verifica si la tarjeta tiene datos de emisión asignados
     */
    public boolean hasIssuedData() {
        return cardNumber != null
                && cvv != null
                && pinHash != null
                && expirationDate != null;
    }

    /**
     * Obtiene los últimos 4 dígitos de la tarjeta (para logs seguros)
     */
    public String getLastFourDigits() {
        return cardNumber != null ? cardNumber.getLastFourDigits() : null;
    }
}
