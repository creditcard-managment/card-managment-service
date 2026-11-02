package com.unisono.card.data.application.dto.request;

import com.unisono.card.data.domain.model.CardCategory;
import com.unisono.card.data.domain.model.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO para crear una tarjeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear una nueva tarjeta")
public class CreateCardRequest {

    @NotBlank(message = "El ID del cliente es obligatorio")
    @Size(min = 7, max = 11, message = "El ID del cliente debe tener entre 7 y 11 dígitos")
    @Schema(description = "ID del cliente (DNI, CUIL)", example = "12345678")
    private String clientId;

    // Datos del titular
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Schema(description = "Nombre del titular", example = "Juan")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    @Schema(description = "Apellido del titular", example = "Pérez")
    private String lastName;

    @NotBlank(message = "El documento es obligatorio")
    @Schema(description = "Número de documento del titular", example = "12345678")
    private String documentNumber;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    @Schema(description = "Email del titular", example = "juan.perez@example.com")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "El formato del teléfono es inválido")
    @Schema(description = "Teléfono del titular", example = "+5491123456789")
    private String phoneNumber;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @Schema(description = "Fecha de nacimiento del titular", example = "1990-01-15")
    private LocalDate birthDate;

    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    @Schema(description = "Dirección del titular", example = "Av. Corrientes 1234, CABA")
    private String address;

    // Tipo y categoría de tarjeta
    @NotNull(message = "El tipo de tarjeta es obligatorio")
    @Schema(description = "Tipo de tarjeta", example = "VISA")
    private CardType type;

    @NotNull(message = "La categoría de tarjeta es obligatoria")
    @Schema(description = "Categoría de tarjeta", example = "CLASSIC")
    private CardCategory category;
}
