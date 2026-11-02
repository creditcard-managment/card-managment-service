package com.unisono.card.lifecycle.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity JPA para state_transitions
 */
@Entity
@Table(name = "state_transitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Column(name = "from_state", length = 20)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 20)
    private String toState;

    @Column(name = "event", nullable = false, length = 20)
    private String event;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "transitioned_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime transitionedAt;

    @Column(name = "successful", nullable = false)
    @Builder.Default
    private Boolean successful = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // Para guardar datos adicionales en formato JSON
}
