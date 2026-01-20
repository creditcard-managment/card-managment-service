package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.fees.FeeStructureId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Estructura de fees actualizada
 */
@Getter
public class FeeStructureUpdated implements MerchantDomainEvent {

    private final String eventId;
    private final MerchantId merchantId;
    private final FeeStructureId oldFeeStructureId;
    private final FeeStructureId newFeeStructureId;
    private final BigDecimal oldMonthlyFee;
    private final BigDecimal newMonthlyFee;
    private final LocalDateTime effectiveFrom;
    private final String updatedBy;
    private final LocalDateTime occurredOn;
    private final Integer version;

    private FeeStructureUpdated(
            MerchantId merchantId,
            FeeStructureId oldFeeStructureId,
            FeeStructureId newFeeStructureId,
            BigDecimal oldMonthlyFee,
            BigDecimal newMonthlyFee,
            LocalDateTime effectiveFrom,
            String updatedBy
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.merchantId = Objects.requireNonNull(merchantId);
        this.oldFeeStructureId = oldFeeStructureId;
        this.newFeeStructureId = Objects.requireNonNull(newFeeStructureId);
        this.oldMonthlyFee = oldMonthlyFee;
        this.newMonthlyFee = newMonthlyFee;
        this.effectiveFrom = effectiveFrom != null ? effectiveFrom : LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public static FeeStructureUpdated of(
            MerchantId merchantId,
            FeeStructureId oldFeeStructureId,
            FeeStructureId newFeeStructureId,
            BigDecimal oldMonthlyFee,
            BigDecimal newMonthlyFee,
            String updatedBy
    ) {
        return new FeeStructureUpdated(
                merchantId,
                oldFeeStructureId,
                newFeeStructureId,
                oldMonthlyFee,
                newMonthlyFee,
                LocalDateTime.now(),
                updatedBy
        );
    }

    @Override
    public String getEventType() {
        return "FeeStructureUpdated";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "FeeStructureUpdated{" +
                "merchantId=" + merchantId +
                ", newFeeStructureId=" + newFeeStructureId +
                ", effectiveFrom=" + effectiveFrom +
                '}';
    }
}
