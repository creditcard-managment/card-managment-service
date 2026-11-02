package com.unisono.card.data.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request DTO para cargar los datos de emisión de una tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de emisión de una tarjeta (PAN, CVV, PIN y vencimiento)")
public class UpdateCardDataRequest {

    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "^\\d{13,19}$", message = "El número de tarjeta debe tener entre 13 y 19 dígitos")
    @Schema(description = "Número de tarjeta (PAN)", example = "4532015112830366")
    @ToString.Exclude
    private String cardNumber;

    @NotBlank(message = "El CVV es obligatorio")
    @Pattern(regexp = "^\\d{3,4}$", message = "El CVV debe tener 3 o 4 dígitos")
    @Schema(description = "CVV", example = "123")
    @ToString.Exclude
    private String cvv;

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener 4 dígitos")
    @Schema(description = "PIN de 4 dígitos", example = "4821")
    @ToString.Exclude
    private String pin;

    @NotNull(message = "El mes de vencimiento es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    @Schema(description = "Mes de vencimiento", example = "12")
    private Integer expirationMonth;

    @NotNull(message = "El año de vencimiento es obligatorio")
    @Schema(description = "Año de vencimiento (4 dígitos)", example = "2030")
    private Integer expirationYear;
}
