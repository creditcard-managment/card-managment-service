package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.ContractId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Contrato terminado
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractTerminated implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final ContractId contractId;
    private final String reason;
    private final LocalDateTime terminatedAt;
    private final LocalDateTime occurredOn;
    private final Integer version;

    public static ContractTerminated of(
            MerchantId merchantId,
            ContractId contractId,
            String reason
    ) {
        Objects.requireNonNull(merchantId, "MerchantId cannot be null");
        Objects.requireNonNull(contractId, "ContractId cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");

        return new ContractTerminated(
                UUID.randomUUID().toString(),
                merchantId,
                contractId,
                reason,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
        );
    }

    @Override
    public String getEventType() {
        return "ContractTerminated";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "ContractTerminated{" +
                "eventId='" + eventId + '\'' +
                ", merchantId=" + merchantId +
                ", contractId=" + contractId +
                ", reason='" + reason + '\'' +
                ", terminatedAt=" + terminatedAt +
                '}';
    }
}
