package com.unisono.card.merchant.application.dto.request;

import com.unisono.card.merchant.domain.model.MerchantCategory;
import com.unisono.card.merchant.domain.model.MerchantType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request para registrar un nuevo merchant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMerchantRequest {

    // Business Information
    @NotBlank(message = "Legal name is required")
    @Size(min = 3, max = 300, message = "Legal name must be between 3 and 300 characters")
    private String legalName;

    @Size(max = 300, message = "Trade name must not exceed 300 characters")
    private String tradeName;

    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[0-9]{8,15}$", message = "Invalid registration number format")
    private String registrationNumber;

    @Size(max = 1000, message = "Business description must not exceed 1000 characters")
    private String businessDescription;

    @Min(value = 0, message = "Years in business cannot be negative")
    @Max(value = 200, message = "Years in business seems unrealistic")
    private Integer yearsInBusiness;

    // Tax Information
    @NotBlank(message = "Tax ID (CUIT) is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "Invalid CUIT format (must be 11 digits)")
    private String taxId;

    @NotBlank(message = "Tax category is required")
    private String taxCategory;

    private boolean vatResponsible;

    private String taxAddress;

    // Contact Information
    @NotBlank(message = "Contact name is required")
    @Size(min = 2, max = 200, message = "Contact name must be between 2 and 200 characters")
    private String contactName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[+]?[0-9]{8,20}$", message = "Invalid phone format")
    private String contactPhone;

    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
            message = "Invalid website URL")
    private String website;

    // Address
    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "Street number is required")
    private String streetNumber;

    private String floor;

    private String apartment;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State/Province is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[A-Z0-9]{4,10}$", message = "Invalid postal code format")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    // Merchant Classification
    @NotNull(message = "Merchant category is required")
    private MerchantCategory category;

    @NotNull(message = "Merchant type is required")
    private MerchantType type;
}
