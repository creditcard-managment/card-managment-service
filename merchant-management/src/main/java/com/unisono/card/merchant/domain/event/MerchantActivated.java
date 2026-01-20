package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Merchant activado
 */
@Getter
public class MerchantActivated implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final LocalDateTime activatedAt;
    private final String activatedBy;
    private final LocalDateTime occurredOn;
    private final Integer version;

    @Builder
    private MerchantActivated(
            MerchantId merchantId,
            LocalDateTime activatedAt,
            String activatedBy
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.merchantId = Objects.requireNonNull(merchantId);
        this.activatedAt = activatedAt != null ? activatedAt : LocalDateTime.now();
        this.activatedBy = activatedBy;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public static MerchantActivated of(MerchantId id, LocalDateTime activatedAt, String activatedBy) {
        return new MerchantActivated(
                id,
                activatedAt,
                activatedBy
        );
    }

    @Override
    public String getEventType() {
        return "MerchantActivated";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "MerchantActivated{" +
                "merchantId=" + merchantId +
                ", activatedAt=" + activatedAt +
                ", activatedBy='" + activatedBy + '\'' +
                '}';
    }
}
