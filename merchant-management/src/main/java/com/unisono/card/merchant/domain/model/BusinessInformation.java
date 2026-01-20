package com.unisono.card.merchant.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Información legal y comercial del negocio
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessInformation {

    private String legalName;
    private String tradeName;
    private String registrationNumber; // CUIT/RUT/Tax ID
    private String businessDescription;
    private Integer yearsInBusiness;

    public static BusinessInformation of(
            String legalName,
            String tradeName,
            String registrationNumber,
            String businessDescription,
            Integer yearsInBusiness
    ) {
        validateLegalName(legalName);
        validateRegistrationNumber(registrationNumber);
        validateYearsInBusiness(yearsInBusiness);

        return new BusinessInformation(
                legalName.trim(),
                tradeName != null ? tradeName.trim() : legalName.trim(),
                registrationNumber.trim(),
                businessDescription != null ? businessDescription.trim() : null,
                yearsInBusiness
        );
    }

    private static void validateLegalName(String legalName) {
        Objects.requireNonNull(legalName, "Legal name cannot be null");
        if (legalName.trim().isEmpty()) {
            throw new IllegalArgumentException("Legal name cannot be empty");
        }
        if (legalName.length() > 300) {
            throw new IllegalArgumentException("Legal name too long (max 300 characters)");
        }
    }

    private static void validateRegistrationNumber(String registrationNumber) {
        Objects.requireNonNull(registrationNumber, "Registration number cannot be null");
        String cleaned = registrationNumber.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Registration number must contain digits");
        }
    }

    private static void validateYearsInBusiness(Integer years) {
        if (years != null && years < 0) {
            throw new IllegalArgumentException("Years in business cannot be negative");
        }
    }

    public boolean isEstablished() {
        return yearsInBusiness != null && yearsInBusiness >= 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BusinessInformation)) return false;
        BusinessInformation that = (BusinessInformation) o;
        return Objects.equals(registrationNumber, that.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationNumber);
    }

    @Override
    public String toString() {
        return "BusinessInformation{" +
                "legalName='" + legalName + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                '}';
    }
}
