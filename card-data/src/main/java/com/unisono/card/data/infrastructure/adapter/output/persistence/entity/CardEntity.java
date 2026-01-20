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
    @Column(name = "card_id")
    private String cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    // Datos de emisión
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "cvv", length = 4)
    private String cvv;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "expiration_month")
    private Integer expirationMonth;

    @Column(name = "expiration_year")
    private Integer expirationYear;

    // ✅ Campos de CardHolder (desnormalizados)
    @Column(name = "holder_first_name")
    private String holderFirstName;

    @Column(name = "holder_last_name")
    private String holderLastName;

    @Column(name = "holder_document_number")
    private String holderDocumentNumber;

    @Column(name = "holder_email")
    private String holderEmail;

    @Column(name = "holder_phone_number")
    private String holderPhoneNumber;

    @Column(name = "holder_birth_date")
    private LocalDate holderBirthDate;

    @Column(name = "holder_address", length = 500)
    private String holderAddress;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
