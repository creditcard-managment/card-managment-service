package com.unisono.card.merchant.domain.model;

import lombok.Getter;

/**
 * Categorías de comercio según MCC (Merchant Category Code)
 * Basado en estándares internacionales
 */
@Getter
public enum MerchantCategory {

    // Retail
    RETAIL_GENERAL("5399", "General Retail", "Comercio minorista general"),
    SUPERMARKET("5411", "Supermarket", "Supermercado"),
    CONVENIENCE_STORE("5499", "Convenience Store", "Tienda de conveniencia"),
    CLOTHING("5651", "Clothing Store", "Tienda de ropa"),
    SHOE_STORE("5661", "Shoe Store", "Zapatería"),
    JEWELRY("5944", "Jewelry Store", "Joyería"),
    ELECTRONICS("5732", "Electronics Store", "Tienda de electrónica"),
    FURNITURE("5712", "Furniture Store", "Mueblería"),
    HARDWARE("5251", "Hardware Store", "Ferretería"),
    BOOKSTORE("5942", "Book Store", "Librería"),
    PHARMACY("5912", "Pharmacy", "Farmacia"),

    // Food & Beverage
    RESTAURANT("5812", "Restaurant", "Restaurante"),
    FAST_FOOD("5814", "Fast Food", "Comida rápida"),
    CAFE("5813", "Café/Bar", "Café/Bar"),
    CATERING("5811", "Catering Service", "Servicio de catering"),

    // Services
    HOTEL("7011", "Hotel/Motel", "Hotel/Motel"),
    TRAVEL_AGENCY("4722", "Travel Agency", "Agencia de viajes"),
    CAR_RENTAL("7512", "Car Rental", "Alquiler de autos"),
    PARKING("7523", "Parking", "Estacionamiento"),
    GAS_STATION("5541", "Gas Station", "Estación de servicio"),
    AUTO_REPAIR("7538", "Auto Repair", "Taller mecánico"),

    // Entertainment
    CINEMA("7832", "Cinema", "Cine"),
    THEATER("7922", "Theater", "Teatro"),
    GYM("7997", "Gym/Fitness", "Gimnasio"),
    SPORTS("7941", "Sports Club", "Club deportivo"),

    // Healthcare
    DOCTOR("8011", "Doctor", "Consultorio médico"),
    DENTIST("8021", "Dentist", "Consultorio odontológico"),
    HOSPITAL("8062", "Hospital", "Hospital"),
    VETERINARY("0742", "Veterinary", "Veterinaria"),

    // Professional Services
    LEGAL_SERVICES("8111", "Legal Services", "Servicios legales"),
    ACCOUNTING("8931", "Accounting", "Servicios contables"),
    CONSULTING("7392", "Consulting", "Consultoría"),

    // Education
    SCHOOL("8211", "School", "Escuela"),
    UNIVERSITY("8220", "University", "Universidad"),

    // E-commerce
    ECOMMERCE_GENERAL("5999", "E-commerce General", "E-commerce general"),
    DIGITAL_GOODS("5815", "Digital Goods", "Productos digitales"),

    // Utilities
    UTILITIES("4900", "Utilities", "Servicios públicos"),
    TELECOM("4814", "Telecommunications", "Telecomunicaciones"),

    // Other
    GOVERNMENT("9399", "Government Services", "Servicios gubernamentales"),
    CHARITY("8398", "Charity/Non-Profit", "ONG/Sin fines de lucro"),
    OTHER("9999", "Other", "Otros");

    private final String mccCode;
    private final String description;
    private final String descriptionEs;

    MerchantCategory(String mccCode, String description, String descriptionEs) {
        this.mccCode = mccCode;
        this.description = description;
        this.descriptionEs = descriptionEs;
    }

    public static MerchantCategory fromMccCode(String mccCode) {
        for (MerchantCategory category : values()) {
            if (category.mccCode.equals(mccCode)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown MCC code: " + mccCode);
    }

    public boolean isHighRisk() {
        return this == JEWELRY ||
                this == ECOMMERCE_GENERAL ||
                this == DIGITAL_GOODS ||
                this == TRAVEL_AGENCY;
    }

    public boolean requiresSpecialApproval() {
        return this == GOVERNMENT ||
                this == CHARITY ||
                this == LEGAL_SERVICES;
    }
}
