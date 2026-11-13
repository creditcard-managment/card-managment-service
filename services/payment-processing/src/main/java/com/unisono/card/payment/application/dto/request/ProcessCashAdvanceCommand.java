package com.unisono.card.payment.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Command for processing a cash advance transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessCashAdvanceCommand {

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotBlank(message = "Card number token is required")
    private String cardNumberToken;

    @NotBlank(message = "Last four digits are required")
    @Pattern(regexp = "\\d{4}", message = "Last four digits must be exactly 4 numeric characters")
    private String lastFourDigits;

    @NotBlank(message = "Expiration date is required")
    @Pattern(regexp = "(0[1-9]|1[0-2])/20\\d{2}", message = "Expiration date must be in format MM/YYYY")
    private String expirationDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "5000.00", message = "Cash advance exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotBlank(message = "ATM/Terminal ID is required")
    private String terminalId;

    private String atmLocation;

    @Pattern(regexp = "[A-Z]{2}", message = "Country code must be ISO 3166-1 alpha-2")
    private String country;

    /**
     * PIN cifrado por el terminal: PIN block ISO 9564 formato 4 (AES). Se reenvía tal cual a card-data
     * (POST /cards/{id}/validate-pin), que lo descifra y lo verifica. Nunca viaja ni se guarda en claro.
     */
    @NotBlank(message = "PIN block is required for cash advance")
    @Pattern(regexp = "[0-9A-Fa-f]{32}", message = "PIN block must be 32 hexadecimal characters (ISO 9564 format 4)")
    private String pinBlock;

    @NotBlank(message = "PIN key id is required for cash advance")
    @Size(max = 32, message = "PIN key id must be at most 32 characters")
    private String pinKeyId;
}
