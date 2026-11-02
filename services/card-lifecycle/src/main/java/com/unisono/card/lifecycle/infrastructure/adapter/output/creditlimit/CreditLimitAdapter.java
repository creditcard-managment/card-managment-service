package com.unisono.card.lifecycle.infrastructure.adapter.output.creditlimit;

import com.unisono.card.lifecycle.domain.port.output.CreditLimitPort;
import com.unisono.card.shared.domain.valueobject.CardId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que accede directamente a la tabla credit_limits
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditLimitAdapter implements CreditLimitPort {

    private final CreditLimitJpaRepository repository; // ✅ Repository propio

    @Override
    public boolean hasLimitAssigned(CardId cardId) {
        log.debug("Verificando si tarjeta tiene límite asignado: {}", cardId.getValue());

        boolean hasLimit = repository.existsByCardId(cardId.getValue());

        log.debug("Tarjeta {} {} límite asignado",
                cardId.getValue(),
                hasLimit ? "tiene" : "no tiene");

        return hasLimit;
    }

    @Override
    public boolean canActivate(CardId cardId) {
        log.debug("Verificando si tarjeta puede ser activada: {}", cardId.getValue());

        // Para activar, debe tener límite asignado
        boolean hasLimit = hasLimitAssigned(cardId);

        if (!hasLimit) {
            log.warn("Tarjeta no puede ser activada - no tiene límite asignado: {}",
                    cardId.getValue());
        }

        return hasLimit;
    }
}
