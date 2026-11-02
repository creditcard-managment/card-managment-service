package com.unisono.card.payment.application.dto.request;

import com.unisono.card.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command for processing a payment transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotBlank(message = "Card number token is required")
    private String cardNumberToken;

    @NotBlank(message = "Last four digits are required")
    @Pattern(regexp = "\\d{4}", message = "Last four digits must be exactly 4 numeric characters")
    private String lastFourDigits;

    private String cardholderName;

    @NotBlank(message = "Expiration date is required")
    @Pattern(regexp = "(0[1-9]|1[0-2])/20\\d{2}", message = "Expiration date must be in format MM/YYYY")
    private String expirationDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "50000.00", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
    private String currency;

    // Optional: for currency conversion
    private BigDecimal localAmount;
    private String localCurrency;
    private BigDecimal exchangeRate;

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @Pattern(regexp = "\\d{4}", message = "MCC must be 4 digits")
    private String merchantCategoryCode;

    private String merchantCity;

    @Pattern(regexp = "[A-Z]{2}", message = "Country code must be ISO 3166-1 alpha-2")
    private String merchantCountry;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String terminalId;
    private String posEntryMode;

    // CVV (only for validation, never stored)
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3 or 4 digits")
    private String cvv;
}
