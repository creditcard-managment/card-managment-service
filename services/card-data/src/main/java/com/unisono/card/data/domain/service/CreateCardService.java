package com.unisono.card.data.domain.service;

import com.unisono.card.data.application.dto.request.CreateCardRequest;
import com.unisono.card.data.domain.model.Card;
import com.unisono.card.data.domain.model.CardHolder;
import com.unisono.card.data.domain.port.output.CardRepository;
import com.unisono.card.data.infrastructure.adapter.output.event.KafkaEventPublisher;
import com.unisono.card.events.config.KafkaTopics;
import com.unisono.card.events.domain.CardCreatedEvent;
import com.unisono.card.shared.domain.valueobject.ClientId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCardService {

    private final CardRepository cardRepository;
    private final KafkaEventPublisher eventPublisher;

    @Transactional
    public Card createCard(CreateCardRequest request) {
        log.info("🔵 Creating card for client: {}", request.getClientId());

        try {
            // Construir Value Objects
            ClientId clientId = ClientId.of(request.getClientId());

            CardHolder holder = CardHolder.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .documentNumber(request.getDocumentNumber())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .birthDate(request.getBirthDate())
                    .address(request.getAddress())
                    .build();

            log.info("🔵 CardHolder created: {}", holder.getFullName());

            // Crear la tarjeta
            Card card = Card.create(clientId, holder, request.getType(), request.getCategory());
            log.info("🔵 Card domain object created with ID: {}", card.getId().getValue());

            // Guardar en BD
            log.info("🔵 About to save card to database...");
            Card savedCard = cardRepository.save(card);
            log.info("🔵 ✅ Card saved to database: {}", savedCard.getId().getValue());

            // ✅ PUBLICAR EVENTO
            log.info("🔵 About to publish CardCreatedEvent...");
            publishCardCreatedEvent(savedCard);
            log.info("🔵 ✅ CardCreatedEvent published");

            log.info("🔵 Card created successfully: {}", savedCard.getId().getValue());
            return savedCard;

        } catch (Exception e) {
            log.error("🔴 ❌ ERROR in createCard: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void publishCardCreatedEvent(Card card) {
        CardCreatedEvent event = new CardCreatedEvent(
                card.getId().getValue(),
                card.getClientId().getValue(),
                card.getHolder().getFullName(),
                card.getType().name(),
                card.getCategory().name(),
                "system"  // createdBy
        );

        eventPublisher.publish(
                KafkaTopics.CARD_CREATED,
                card.getId().getValue(),
                event
        );

        log.info("📤 CardCreatedEvent published for card: {}", card.getId().getValue());
    }
}
