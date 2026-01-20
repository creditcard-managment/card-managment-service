package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.contract.ContractId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Contrato renovado
 */
@Getter
public class ContractRenewed implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final ContractId oldContractId;
    private final ContractId newContractId;
    private final LocalDate oldEndDate;
    private final LocalDate newEndDate;
    private final LocalDateTime occurredOn;
    private final Integer version;

    private ContractRenewed(
            MerchantId merchantId,
            ContractId oldContractId,
            ContractId newContractId,
            LocalDate oldEndDate,
            LocalDate newEndDate
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.merchantId = Objects.requireNonNull(merchantId);
        this.oldContractId = oldContractId;
        this.newContractId = Objects.requireNonNull(newContractId);
        this.oldEndDate = oldEndDate;
        this.newEndDate = Objects.requireNonNull(newEndDate);
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public static ContractRenewed of(
            MerchantId merchantId,
            ContractId oldContractId,
            ContractId newContractId,
            LocalDate oldEndDate,
            LocalDate newEndDate
    ) {
        return new ContractRenewed(
                merchantId,
                oldContractId,
                newContractId,
                oldEndDate,
                newEndDate
        );
    }

    @Override
    public String getEventType() {
        return "ContractRenewed";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "ContractRenewed{" +
                "merchantId=" + merchantId +
                ", newContractId=" + newContractId +
                ", newEndDate=" + newEndDate +
                '}';
    }
}
