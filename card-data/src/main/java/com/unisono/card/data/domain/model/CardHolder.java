package com.unisono.card.data.domain.model;

import com.unisono.card.shared.domain.exception.ValidationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity: CardHolder
 *
 * Representa al titular de la tarjeta.
 * Contiene información personal del cliente.
 */
@Getter
@Builder
@AllArgsConstructor
public class CardHolder {

    private String firstName;
    private String lastName;
    private String documentNumber;  // DNI, CUIL, etc.
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;

    // ========================================
    // FACTORY METHOD
    // ========================================

    public static CardHolder create(
            String firstName,
            String lastName,
            String documentNumber,
            String email,
            String phoneNumber,
            LocalDate birthDate
    ) {
        validate(firstName, lastName, documentNumber, email, phoneNumber, birthDate);

        return CardHolder.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .documentNumber(documentNumber.trim())
                .email(email.trim().toLowerCase())
                .phoneNumber(phoneNumber.trim())
                .birthDate(birthDate)
                .build();
    }

    // ========================================
    // BUSINESS LOGIC
    // ========================================

    /**
     * Obtiene el nombre completo
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Obtiene el nombre como aparece en la tarjeta (mayúsculas)
     */
    public String getCardName() {
        return getFullName().toUpperCase();
    }

    /**
     * Verifica si el titular es mayor de edad
     */
    public boolean isAdult() {
        return birthDate.plusYears(18).isBefore(LocalDate.now())
                || birthDate.plusYears(18).isEqual(LocalDate.now());
    }

    /**
     * Calcula la edad actual
     */
    public int getAge() {
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    /**
     * Actualiza el email
     */
    public void updateEmail(String newEmail) {
        if (StringUtils.isBlank(newEmail)) {
            throw new ValidationException("El email no puede ser vacío");
        }
        if (!isValidEmail(newEmail)) {
            throw new ValidationException("El formato del email es inválido");
        }
        this.email = newEmail.trim().toLowerCase();
    }

    /**
     * Actualiza el teléfono
     */
    public void updatePhoneNumber(String newPhoneNumber) {
        if (StringUtils.isBlank(newPhoneNumber)) {
            throw new ValidationException("El teléfono no puede ser vacío");
        }
        this.phoneNumber = newPhoneNumber.trim();
    }

    /**
     * Actualiza la dirección
     */
    public void updateAddress(String newAddress) {
        if (StringUtils.isBlank(newAddress)) {
            throw new ValidationException("La dirección no puede ser vacía");
        }
        this.address = newAddress.trim();
    }

    // ========================================
    // VALIDATIONS
    // ========================================

    private static void validate(
            String firstName,
            String lastName,
            String documentNumber,
            String email,
            String phoneNumber,
            LocalDate birthDate
    ) {
        if (StringUtils.isBlank(firstName)) {
            throw new ValidationException("El nombre es obligatorio");
        }
        if (StringUtils.isBlank(lastName)) {
            throw new ValidationException("El apellido es obligatorio");
        }
        if (StringUtils.isBlank(documentNumber)) {
            throw new ValidationException("El número de documento es obligatorio");
        }
        if (StringUtils.isBlank(email)) {
            throw new ValidationException("El email es obligatorio");
        }
        if (!isValidEmail(email)) {
            throw new ValidationException("El formato del email es inválido");
        }
        if (StringUtils.isBlank(phoneNumber)) {
            throw new ValidationException("El teléfono es obligatorio");
        }
        if (birthDate == null) {
            throw new ValidationException("La fecha de nacimiento es obligatoria");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de nacimiento no puede ser futura");
        }

        // Validar que sea mayor de edad
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        if (birthDate.isAfter(eighteenYearsAgo)) {
            throw new ValidationException("El titular debe ser mayor de 18 años");
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }


    // ========================================
    // EQUALS & HASHCODE
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardHolder that = (CardHolder) o;
        return Objects.equals(documentNumber, that.documentNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "CardHolder{name='%s', document='%s', email='%s'}",
                getFullName(), documentNumber, email
        );
    }
}
