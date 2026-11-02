package com.unisono.card.data.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

/**
 * Embeddable para CardHolder
 *
 * Los datos del titular se almacenan en la misma tabla de la tarjeta.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardHolderEmbeddable {

    @Column(name = "holder_first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "holder_last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "holder_document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "holder_email", nullable = false, length = 255)
    private String email;

    @Column(name = "holder_phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "holder_birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "holder_address", length = 500)
    private String address;
}
