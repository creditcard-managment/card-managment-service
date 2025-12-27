package com.unisono.card.data.infrastructure.adapter.output.persistence.entity;

import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardStatus;
import com.unisono.card.data.domain.model.CardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.YearMonth;

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

    // Datos del titular (embedded)
    @Embedded
    private CardHolderEmbeddable holder;

    // Datos sensibles (ENCRIPTADOS en BD)
    @Column(name = "card_number_encrypted", nullable = true, columnDefinition = "TEXT")
    private String cardNumberEncrypted;

    @Column(name = "last_four_digits", nullable = true, length = 4)
    private String lastFourDigits;  // Para búsquedas y display

    @Column(name = "cvv_encrypted", nullable = true, columnDefinition = "TEXT")
    private String cvvEncrypted;

    @Column(name = "pin_hash", nullable = true, columnDefinition = "TEXT")
    private String pinHash;  // BCrypt hash

    // Metadatos de la tarjeta
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CardType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private CardCategory category;

    @Column(name = "expiration_month", nullable = true)
    private Integer expirationMonth;

    @Column(name = "expiration_year", nullable = true)
    private Integer expirationYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    // Auditoría
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

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    // Soft delete
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // Helper methods
    // ========================================

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
