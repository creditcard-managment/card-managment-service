package com.unisono.card.data.infrastructure.adapter.output.persistence.adapter;

import com.unisono.card.data.domain.port.output.ProcessedEventPort;
import com.unisono.card.data.infrastructure.adapter.output.persistence.entity.ProcessedEventEntity;
import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementa ProcessedEventPort con card_data_processed_events.
 *
 * Si dos consumos del mismo evento corren a la vez, la PK (event_id) hace fallar al segundo; su mensaje se
 * reintenta y en el reintento ya figura como procesado.
 */
@Component
@RequiredArgsConstructor
public class ProcessedEventAdapter implements ProcessedEventPort {

    private final JpaProcessedEventRepository repository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String eventId, String topic) {
        if (repository.existsById(eventId)) {
            return false;
        }
        repository.saveAndFlush(new ProcessedEventEntity(eventId, topic, LocalDateTime.now()));
        return true;
    }
}
