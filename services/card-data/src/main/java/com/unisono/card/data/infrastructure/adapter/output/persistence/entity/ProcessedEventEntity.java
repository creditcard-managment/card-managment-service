package com.unisono.card.data.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de Kafka ya procesado por card-data (tabla card_data_processed_events, V16)
 */
@Entity
@Table(name = "card_data_processed_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
