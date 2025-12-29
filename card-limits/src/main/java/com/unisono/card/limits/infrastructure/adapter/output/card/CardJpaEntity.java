package com.unisono.card.limits.infrastructure.adapter.output.card;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity simplificado - solo campos necesarios para card-limits
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardJpaEntity {

    @Id
    private String id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
