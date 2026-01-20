package com.unisono.card.merchant.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Información fiscal del merchant
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaxInformation {

    private String taxId; // CUIT en Argentina
    private String taxCategory; // Monotributista, Responsable Inscripto, etc.
    private boolean vatResponsible; // IVA
    private String taxAddress;

    public static TaxInformation of(
            String taxId,
            String taxCategory,
            boolean vatResponsible,
            String taxAddress
    ) {
        validateTaxId(taxId);
        Objects.requireNonNull(taxCategory, "Tax category cannot be null");

        return new TaxInformation(
                taxId.trim(),
                taxCategory.trim(),
                vatResponsible,
                taxAddress != null ? taxAddress.trim() : null
        );
    }

    private static void validateTaxId(String taxId) {
        Objects.requireNonNull(taxId, "Tax ID cannot be null");
        String cleaned = taxId.replaceAll("[^0-9]", "");

        // Validación CUIT Argentina (11 dígitos)
        if (cleaned.length() != 11) {
            throw new IllegalArgumentException("Invalid Tax ID format (expected 11 digits for CUIT)");
        }
    }

    public String getFormattedTaxId() {
        String cleaned = taxId.replaceAll("[^0-9]", "");
        // Formato: XX-XXXXXXXX-X
        if (cleaned.length() == 11) {
            return cleaned.substring(0, 2) + "-" +
                    cleaned.substring(2, 10) + "-" +
                    cleaned.substring(10);
        }
        return taxId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaxInformation)) return false;
        TaxInformation that = (TaxInformation) o;
        return Objects.equals(taxId, that.taxId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxId);
    }

    @Override
    public String toString() {
        return "TaxInformation{" +
                "taxId='" + getFormattedTaxId() + '\'' +
                ", taxCategory='" + taxCategory + '\'' +
                ", vatResponsible=" + vatResponsible +
                '}';
    }
}
