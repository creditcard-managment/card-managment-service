package com.unisono.card.data.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para validar PIN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para validar PIN de una tarjeta")
public class ValidatePINRequest {

    @Pattern(regexp = "^[0-9A-Fa-f]{32}$", message = "El PIN block debe tener 32 caracteres hexadecimales")
    @Schema(description = "PIN cifrado en un PIN block ISO 9564 formato 4 (AES), en hexadecimal",
            example = "8E2C1F0A5B3D4E6F7A8B9C0D1E2F3A4B")
    private String pinBlock;

    @Size(max = 32, message = "El id de la clave de PIN no puede superar 32 caracteres")
    @Schema(description = "Id de la clave con la que se cifró el PIN block (pin-encryption.keys)", example = "pek-v1")
    private String pinKeyId;

    @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener 4 dígitos")
    @Schema(description = "PIN en claro. Solo para clientes internos que todavía no cifran (activación "
            + "desde card-lifecycle); usar pinBlock", example = "1234", deprecated = true)
    private String pin;

    @JsonIgnore
    @AssertTrue(message = "Enviar pinBlock con pinKeyId, o pin (no ambos)")
    public boolean isPinOrPinBlock() {
        boolean hasBlock = pinBlock != null && !pinBlock.isBlank();
        boolean hasKey = pinKeyId != null && !pinKeyId.isBlank();
        boolean hasPin = pin != null && !pin.isBlank();
        return hasBlock ? hasKey && !hasPin : hasPin && !hasKey;
    }

    @Override
    public String toString() {
        return "ValidatePINRequest[pinBlock=" + (pinBlock != null) + ", pinKeyId=" + pinKeyId
                + ", pin=" + (pin != null ? "****" : null) + "]";
    }
}
