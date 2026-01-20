package com.unisono.card.lifecycle.infrastructure.adapter.output.card;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity JPA para card-lifecycle
 * Incluye campos de emisión que se asignan cuando se emite la tarjeta
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardJpaEntity {

    @Id
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // ✅ NUEVOS: Datos de emisión
    @Column(name = "card_number_encrypted")
    private String cardNumberEncrypted;

    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;

    @Column(name = "cvv_encrypted")
    private String cvvEncrypted;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "expiration_month")
    private Integer expirationMonth;

    @Column(name = "expiration_year")
    private Integer expirationYear;
}