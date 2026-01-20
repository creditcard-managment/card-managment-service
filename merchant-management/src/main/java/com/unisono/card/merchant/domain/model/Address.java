package com.unisono.card.merchant.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Value Object que representa una dirección física
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {

    private String streetAddress;
    private String streetNumber;
    private String floor;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Double latitude;
    private Double longitude;

    public static Address of(
            String streetAddress,
            String streetNumber,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        validateRequired(streetAddress, "Street address");
        validateRequired(streetNumber, "Street number");
        validateRequired(city, "City");
        validateRequired(state, "State");
        validateRequired(postalCode, "Postal code");
        validateRequired(country, "Country");

        return new Address(
                streetAddress.trim(),
                streetNumber.trim(),
                null,
                null,
                city.trim(),
                state.trim(),
                postalCode.trim(),
                country.trim(),
                null,
                null
        );
    }

    public static Address fullAddress(
            String streetAddress,
            String streetNumber,
            String floor,
            String apartment,
            String city,
            String state,
            String postalCode,
            String country,
            Double latitude,
            Double longitude
    ) {
        validateRequired(streetAddress, "Street address");
        validateRequired(streetNumber, "Street number");
        validateRequired(city, "City");
        validateRequired(state, "State");
        validateRequired(postalCode, "Postal code");
        validateRequired(country, "Country");
        validateCoordinates(latitude, longitude);

        return new Address(
                streetAddress.trim(),
                streetNumber.trim(),
                floor != null ? floor.trim() : null,
                apartment != null ? apartment.trim() : null,
                city.trim(),
                state.trim(),
                postalCode.trim(),
                country.trim(),
                latitude,
                longitude
        );
    }

    private static void validateRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }

    private static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Invalid latitude: must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Invalid longitude: must be between -180 and 180");
        }
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(streetAddress).append(" ").append(streetNumber);

        if (floor != null && !floor.isEmpty()) {
            sb.append(", Piso ").append(floor);
        }
        if (apartment != null && !apartment.isEmpty()) {
            sb.append(", Depto ").append(apartment);
        }

        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(", ").append(postalCode);
        sb.append(", ").append(country);

        return sb.toString();
    }

    public boolean hasGeolocation() {
        return latitude != null && longitude != null;
    }

    public Address withGeolocation(Double lat, Double lon) {
        return Address.fullAddress(
                this.streetAddress,
                this.streetNumber,
                this.floor,
                this.apartment,
                this.city,
                this.state,
                this.postalCode,
                this.country,
                lat,
                lon
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return Objects.equals(streetAddress, address.streetAddress) &&
                Objects.equals(streetNumber, address.streetNumber) &&
                Objects.equals(city, address.city) &&
                Objects.equals(postalCode, address.postalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetAddress, streetNumber, city, postalCode);
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
