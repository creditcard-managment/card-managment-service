package com.unisono.card.data.infrastructure.adapter.output.event;


import com.unisono.card.data.domain.event.CardHolderDataUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CardHolderDataUpdatedEventListener {

    private final KafkaTemplate<String, CardHolderDataUpdatedEvent> kafkaTemplate;

    @EventListener
    @Async
    public void handleCardHolderDataUpdated(CardHolderDataUpdatedEvent event) {
        log.info("Processing CardHolderDataUpdatedEvent for card: {}", event.getCardId());

        kafkaTemplate.send("card-events", event.getCardId().getValue(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ CardHolderDataUpdatedEvent published to Kafka for card: {}",
                                event.getCardId());
                    } else {
                        log.error("❌ Error publishing CardHolderDataUpdatedEvent to Kafka: {}",
                                ex.getMessage(), ex);
                    }
                });
    }
}
