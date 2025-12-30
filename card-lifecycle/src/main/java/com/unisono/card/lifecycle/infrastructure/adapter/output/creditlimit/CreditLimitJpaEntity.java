package com.unisono.card.lifecycle.infrastructure.adapter.output.creditlimit;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity simplificado para credit_limits
 * Solo campos necesarios para card-lifecycle
 */
@Entity
@Table(name = "credit_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditLimitJpaEntity {

    @Id
    private String id;

    @Column(name = "card_id", nullable = false, unique = true)
    private String cardId;

    @Column(name = "purchase_limit_amount", nullable = false)
    private BigDecimal purchaseLimitAmount;

    // ✅ SOLO los campos que lifecycle necesita para validar
    // NO necesita todos los campos de card-limits
}
