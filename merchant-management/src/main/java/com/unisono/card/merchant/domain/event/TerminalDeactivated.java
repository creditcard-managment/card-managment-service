package com.unisono.card.merchant.domain.event;

import com.unisono.card.merchant.domain.model.MerchantId;
import com.unisono.card.merchant.domain.model.terminal.TerminalId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: Terminal desactivada
 */
@Getter
public class TerminalDeactivated implements MerchantDomainEvent {

    private final String eventId;
    private final TerminalId terminalId;
    private final MerchantId merchantId;
    private final String reason;
    private final LocalDateTime deactivatedAt;
    private final LocalDateTime occurredOn;
    private final Integer version;

    private TerminalDeactivated(
            TerminalId terminalId,
            MerchantId merchantId,
            String reason,
            LocalDateTime deactivatedAt
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.terminalId = Objects.requireNonNull(terminalId);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.reason = reason;
        this.deactivatedAt = deactivatedAt != null ? deactivatedAt : LocalDateTime.now();
        this.occurredOn = LocalDateTime.now();
        this.version = 1;
    }

    public static TerminalDeactivated of(
            TerminalId terminalId,
            MerchantId merchantId,
            String reason
    ) {
        return new TerminalDeactivated(terminalId, merchantId, reason, LocalDateTime.now());
    }

    @Override
    public String getEventType() {
        return "TerminalDeactivated";
    }

    @Override
    public String getAggregateId() {
        return merchantId.getValue();
    }

    @Override
    public String toString() {
        return "TerminalDeactivated{" +
                "terminalId=" + terminalId +
                ", merchantId=" + merchantId +
                ", reason='" + reason + '\'' +
                '}';
    }
}
