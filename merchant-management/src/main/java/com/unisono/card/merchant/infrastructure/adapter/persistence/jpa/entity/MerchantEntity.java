package com.unisono.card.merchant.infrastructure.adapter.persistence.jpa.entity;

import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantStatus;
import com.unisono.card.merchant.domain.model.MerchantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity para Merchant
 */
@Entity
@Table(
        name = "merchants",
        indexes = {
                @Index(name = "idx_merchant_tax_id", columnList = "tax_id"),
                @Index(name = "idx_merchant_email", columnList = "email"),
                @Index(name = "idx_merchant_status", columnList = "status"),
                @Index(name = "idx_merchant_category", columnList = "category")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantEntity {

    @Id
    @Column(length = 36)
    private String id;

    // Business Information
    @Column(name = "legal_name", nullable = false, length = 300)
    private String legalName;

    @Column(name = "trade_name", length = 300)
    private String tradeName;

    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Column(name = "business_description", length = 1000)
    private String businessDescription;

    @Column(name = "years_in_business")
    private Integer yearsInBusiness;

    // Tax Information
    @Column(name = "tax_id", nullable = false, unique = true, length = 20)
    private String taxId;

    @Column(name = "tax_category", nullable = false, length = 100)
    private String taxCategory;

    @Column(name = "vat_responsible", nullable = false)
    private boolean vatResponsible;

    @Column(name = "tax_address", length = 500)
    private String taxAddress;

    // Contact Information
    @Column(name = "contact_name", nullable = false, length = 200)
    private String contactName;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "website", length = 500)
    private String website;

    // Address
    @Column(name = "street_address", nullable = false, length = 200)
    private String streetAddress;

    @Column(name = "street_number", nullable = false, length = 20)
    private String streetNumber;

    @Column(name = "floor", length = 20)
    private String floor;

    @Column(name = "apartment", length = 20)
    private String apartment;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Classification
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private MerchantCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private MerchantType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MerchantStatus status;

    // Relationships
    @OneToMany(
            mappedBy = "merchant",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PaymentTerminalEntity> terminals = new ArrayList<>();

    @OneToOne(
            mappedBy = "merchant",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private MerchantContractEntity contract;

    // Timestamps
    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    // Optimistic Locking
    @Version
    private Integer version;

    // Helper methods
    public void addTerminal(PaymentTerminalEntity terminal) {
        terminals.add(terminal);
        terminal.setMerchant(this);
    }

    public void removeTerminal(PaymentTerminalEntity terminal) {
        terminals.remove(terminal);
        terminal.setMerchant(null);
    }

    // ⚠️ IMPORTANTE: Método para establecer el contrato bidireccional
    public void setContract(MerchantContractEntity contract) {
        this.contract = contract;
        if (contract != null) {
            contract.setMerchant(this);
        }
    }
}
