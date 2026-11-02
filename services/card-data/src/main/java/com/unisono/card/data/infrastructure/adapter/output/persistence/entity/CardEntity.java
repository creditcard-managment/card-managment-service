package com.unisono.card.data.infrastructure.adapter.output.persistence.entity;

import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.shared.domain.valueobject.CardStatus;
import com.unisono.card.data.domain.model.CardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity para Card
 *
 * Representa la tabla CARDS en la base de datos.
 * Los datos sensibles (card_number, cvv) están encriptados.
 */
@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_client_id", columnList = "client_id"),
        @Index(name = "idx_card_number_encrypted", columnList = "card_number_encrypted"),
        @Index(name = "idx_last_four_digits", columnList = "last_four_digits"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "client_id", nullable = false, length = 20)
    private String clientId;

    // =============================================
    // DATOS DEL TITULAR
    // =============================================

    @Column(name = "holder_first_name", nullable = false, length = 100)
    private String holderFirstName;

    @Column(name = "holder_last_name", nullable = false, length = 100)
    private String holderLastName;

    @Column(name = "holder_document_number", nullable = false, length = 20)
    private String holderDocumentNumber;

    @Column(name = "holder_email", nullable = false, length = 100)
    private String holderEmail;

    @Column(name = "holder_phone_number", length = 20)
    private String holderPhoneNumber;

    @Column(name = "holder_birth_date")
    private LocalDate holderBirthDate;

    @Column(name = "holder_address", length = 500)
    private String holderAddress;

    // =============================================
    // DATOS SENSIBLES (ENCRIPTADOS EN BD)
    // =============================================

    @Column(name = "card_number_encrypted", columnDefinition = "TEXT")
    private String cardNumberEncrypted;

    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;  // Para búsquedas y display

    @Column(name = "cvv_encrypted", columnDefinition = "TEXT")
    private String cvvEncrypted;

    @Column(name = "pin_hash", columnDefinition = "TEXT")
    private String pinHash;  // BCrypt hash

    @Column(name = "pin_failed_attempts", nullable = false)
    @Builder.Default
    private Integer pinFailedAttempts = 0;

    // =============================================
    // METADATOS DE LA TARJETA
    // =============================================

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CardType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private CardCategory category;

    @Column(name = "expiration_month")
    private Integer expirationMonth;

    @Column(name = "expiration_year")
    private Integer expirationYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    // =============================================
    // AUDITORÍA Y FECHAS DE CICLO DE VIDA
    // =============================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "issued_at")  // ✅ AGREGADO
    private LocalDateTime issuedAt;

    @Column(name = "cancelled_at")  // ✅ AGREGADO
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")  // ✅ AGREGADO
    private LocalDateTime expiredAt;

    // =============================================
    // RAZONES DE CAMBIOS DE ESTADO
    // =============================================

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    @Column(name = "cancelled_reason", length = 500)  // ✅ AGREGADO
    private String cancelledReason;

    // =============================================
    // SOFT DELETE
    // =============================================

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // =============================================
    // HELPER METHODS
    // =============================================

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = false;
        }
    }

    /**
     * Marca como eliminado (soft delete)
     */
    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
