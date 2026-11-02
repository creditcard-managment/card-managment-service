package com.unisono.card.lifecycle.infrastructure.adapter.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para emitir tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para emitir una tarjeta")
public class IssueCardRequest {

    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Número de tarjeta inválido (13-19 dígitos)")
    @Schema(description = "Número de tarjeta (PAN)", example = "4532123456789012")
    private String cardNumber;

    @NotBlank(message = "El CVV es obligatorio")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV inválido (3-4 dígitos)")
    @Schema(description = "CVV", example = "123")
    private String cvv;

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN inválido (4 dígitos)")
    @Schema(description = "PIN", example = "1234")
    private String pin;

    @NotNull(message = "El mes de vencimiento es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    @Schema(description = "Mes de vencimiento", example = "12")
    private Integer expirationMonth;

    @NotNull(message = "El año de vencimiento es obligatorio")
    @Min(value = 2025, message = "El año debe ser válido")
    @Schema(description = "Año de vencimiento", example = "2028")
    private Integer expirationYear;

    @NotBlank(message = "La razón es obligatoria")
    @Schema(description = "Razón de emisión", example = "Emisión estándar de tarjeta aprobada")
    private String reason;

    @NotBlank(message = "El emisor es obligatorio")
    @Schema(description = "Usuario que emite", example = "system@bank.com")
    private String issuedBy;
}
