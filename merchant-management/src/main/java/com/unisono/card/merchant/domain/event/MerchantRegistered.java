package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.MerchantType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Merchant registrado
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MerchantRegistered implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final String legalName;
    private final String taxId;
    private final MerchantCategory category;
    private final MerchantType type;
    private final String contactEmail;
    private final LocalDateTime occurredOn;
    private final Integer version;

    // Factory method estático
    public static MerchantRegistered of(
            MerchantId merchantId,
            String legalName,
            String taxId,
            MerchantCategory category,
            MerchantType type,
            String contactEmail
    ) {
        Objects.requireNonNull(merchantId, "MerchantId cannot be null");
        Objects.requireNonNull(legalName, "Legal name cannot be null");
        Objects.requireNonNull(taxId, "Tax ID cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(contactEmail, "Contact email cannot be null");

        return new MerchantRegistered(
                UUID.randomUUID().toString(),
                merchantId,
                legalName,
                taxId,
                category,
                type,
                contactEmail,
                LocalDateTime.now(),
                1
        );
    }

    @Override
    public String getEventType() {
        return "MerchantRegistered";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "MerchantRegistered{" +
                "eventId='" + eventId + '\'' +
                ", merchantId=" + merchantId +
                ", legalName='" + legalName + '\'' +
                ", category=" + category +
                ", occurredOn=" + occurredOn +
                '}';
    }
}
