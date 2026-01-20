package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Merchant suspendido
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MerchantSuspended implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final String reason;
    private final LocalDateTime suspendedAt;
    private final String suspendedBy;
    private final LocalDateTime occurredOn;
    private final Integer version;

    // Factory method simplificado (3 parámetros)
    public static MerchantSuspended of(
            MerchantId merchantId,
            String reason,
            String suspendedBy
    ) {
        Objects.requireNonNull(merchantId, "MerchantId cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");

        return new MerchantSuspended(
                UUID.randomUUID().toString(),
                merchantId,
                reason,
                LocalDateTime.now(),  // suspendedAt
                suspendedBy,
                LocalDateTime.now(),  // occurredOn
                1
        );
    }

    // Factory method completo (4 parámetros) - por si se necesita
    public static MerchantSuspended of(
            MerchantId merchantId,
            String reason,
            LocalDateTime suspendedAt,
            String suspendedBy
    ) {
        Objects.requireNonNull(merchantId, "MerchantId cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");

        return new MerchantSuspended(
                UUID.randomUUID().toString(),
                merchantId,
                reason,
                suspendedAt != null ? suspendedAt : LocalDateTime.now(),
                suspendedBy,
                LocalDateTime.now(),
                1
        );
    }

    @Override
    public String getEventType() {
        return "MerchantSuspended";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "MerchantSuspended{" +
                "eventId='" + eventId + '\'' +
                ", merchantId=" + merchantId +
                ", reason='" + reason + '\'' +
                ", suspendedAt=" + suspendedAt +
                '}';
    }
}
