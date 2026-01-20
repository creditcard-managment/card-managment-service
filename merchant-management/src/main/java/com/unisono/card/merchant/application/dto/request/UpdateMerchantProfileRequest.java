package com.unisono.card.merchant.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request para actualizar perfil de merchant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMerchantProfileRequest {

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

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;
}
