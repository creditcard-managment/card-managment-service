package com.unisono.card.lifecycle.infrastructure.adapter.output.card;

import com.unisono.card.data.domain.model.Card;
import com.unisono.card.lifecycle.domain.port.output.CardPort;
import com.unisono.card.shared.domain.exception.NotFoundException;
import com.unisono.card.shared.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter que accede directamente a la tabla cards
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardAdapter implements CardPort {

    private final CardJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findById(CardId cardId) {
        log.debug("Buscando tarjeta: {}", cardId.getValue());

        return repository.findById(cardId.getValue())
                .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(CardId cardId) {
        log.debug("Verificando si tarjeta existe: {}", cardId.getValue());
        return repository.existsById(cardId.getValue());
    }

    @Override
    @Transactional
    public void save(Card card) {
        log.debug("Persistiendo cambios de tarjeta: {} -> status: {}",
                card.getId().getValue(),
                card.getStatus());

        CardJpaEntity entity = repository.findById(card.getId().getValue())
                .orElseThrow(() -> new NotFoundException(
                        "No se puede persistir cambios: Tarjeta no encontrada " + card.getId().getValue()
                ));

        // Actualizar status (siempre)
        entity.setStatus(card.getStatus().name());

        // ✅ Actualizar datos de emisión (solo si fueron asignados)
        if (card.hasIssuedData()) {
            entity.setCardNumberEncrypted(encryptCardNumber(card.getCardNumber()));
            entity.setLastFourDigits(card.getCardNumber().getLastFourDigits());
            entity.setCvvEncrypted(encryptCVV(card.getCvv()));
            entity.setPinHash(card.getPinHash());

            // ✅ Verificar que expirationDate no sea null antes de acceder
            if (card.getExpirationDate() != null) {
                entity.setExpirationMonth(card.getExpirationDate().getMonth());
                entity.setExpirationYear(card.getExpirationDate().getYear());
            }
        }

        // JPA auto-flush persiste los cambios
        log.debug("Entity actualizado, JPA hará flush automáticamente");
    }

    /**
     * Convierte Entity JPA → Modelo de Dominio
     */
    private Card toDomainModel(CardJpaEntity entity) {
        log.trace("Convirtiendo entity a domain model: {}", entity.getId());

        ClientId clientId = ClientId.of(entity.getClientId());

        Card.CardBuilder builder = Card.builder()
                .id(CardId.of(entity.getId()))
                .status(CardStatus.valueOf(entity.getStatus()))
                .clientId(clientId);

        // Agregar datos de emisión si existen
        if (entity.getCardNumberEncrypted() != null) {
            builder.cardNumber(decryptCardNumber(entity.getCardNumberEncrypted()));
        }
        if (entity.getCvvEncrypted() != null) {
            builder.cvv(decryptCVV(entity.getCvvEncrypted()));
        }
        if (entity.getPinHash() != null) {
            builder.pinHash(entity.getPinHash());
        }
        if (entity.getExpirationMonth() != null && entity.getExpirationYear() != null) {
            builder.expirationDate(
                    ExpirationDate.of(entity.getExpirationMonth(), entity.getExpirationYear())
            );
        }

        return builder.build();
    }

    // =============================================
    // MÉTODOS DE ENCRIPTACIÓN (PLACEHOLDER)
    // =============================================

    /**
     * Encripta el número de tarjeta
     * TODO: Implementar encriptación real (AES-256, etc.)
     */
    private String encryptCardNumber(CardNumber cardNumber) {
        // PLACEHOLDER: En producción, usar un servicio de encriptación real
        log.warn("ADVERTENCIA: Usando encriptación placeholder. Implementar encriptación real en producción.");
        return "ENCRYPTED_" + cardNumber.getValue();
    }

    /**
     * Desencripta el número de tarjeta
     * TODO: Implementar desencriptación real
     */
    private CardNumber decryptCardNumber(String encrypted) {
        // PLACEHOLDER: En producción, usar un servicio de desencriptación real
        String decrypted = encrypted.replace("ENCRYPTED_", "");
        return CardNumber.of(decrypted);
    }

    /**
     * Encripta el CVV
     * TODO: Implementar encriptación real
     */
    private String encryptCVV(CVV cvv) {
        // PLACEHOLDER
        log.warn("ADVERTENCIA: Usando encriptación placeholder para CVV.");
        return "ENCRYPTED_" + cvv.getValue();
    }

    /**
     * Desencripta el CVV
     * TODO: Implementar desencriptación real
     */
    private CVV decryptCVV(String encrypted) {
        // PLACEHOLDER
        String decrypted = encrypted.replace("ENCRYPTED_", "");
        return CVV.of(decrypted);
    }
}
