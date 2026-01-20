package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.terminal.TerminalType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * JPA Entity para Virtual Terminal
 */
@Entity
@DiscriminatorValue("VIRTUAL")
@Getter
@Setter
@NoArgsConstructor
public class VirtualTerminalEntity extends PaymentTerminalEntity {

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_version", length = 20)
    private String apiVersion;

    @ElementCollection
    @CollectionTable(name = "terminal_allowed_ips", joinColumns = @JoinColumn(name = "terminal_id"))
    @Column(name = "ip_address")
    private Set<String> allowedIpAddresses;

    @Column(name = "supports_3d_secure")
    private boolean supports3DSecure;

    @Column(name = "supports_tokenization")
    private boolean supportsTokenization;

    @Column(name = "supports_recurring_payments")
    private boolean supportsRecurringPayments;

    @Column(name = "max_concurrent_transactions")
    private Integer maxConcurrentTransactions;

    @Override
    public TerminalType getTerminalType() {
        return TerminalType.VIRTUAL;
    }
}
