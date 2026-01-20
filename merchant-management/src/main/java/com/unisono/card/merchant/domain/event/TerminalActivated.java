package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Terminal activada
 */
@Getter
public class TerminalActivated implements MerchantDomainEvent {

    private final String eventId;
    private final TerminalId terminalId;
    private final MerchantId merchantId;
    private final TerminalType terminalType;
    private final String serialNumber;
    private final LocalDateTime activatedAt;
    private final LocalDateTime occurredOn;
    private final Integer version;

    private TerminalActivated(
            TerminalId terminalId,
            MerchantId merchantId,
            TerminalType terminalType,
            String serialNumber,
            LocalDateTime activatedAt
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.terminalId = Objects.requireNonNull(terminalId);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.terminalType = Objects.requireNonNull(terminalType);
        this.serialNumber = Objects.requireNonNull(serialNumber);
        this.activatedAt = activatedAt != null ? activatedAt : LocalDateTime.now();
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public static TerminalActivated of(
            TerminalId terminalId,
            MerchantId merchantId,
            TerminalType terminalType,
            String serialNumber
    ) {
        return new TerminalActivated(
                terminalId,
                merchantId,
                terminalType,
                serialNumber,
                LocalDateTime.now()
        );
    }

    @Override
    public String getEventType() {
        return "TerminalActivated";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "TerminalActivated{" +
                "terminalId=" + terminalId +
                ", merchantId=" + merchantId +
                ", type=" + terminalType +
                ", serialNumber='" + serialNumber + '\'' +
                '}';
    }
}
