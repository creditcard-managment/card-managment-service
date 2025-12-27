package com.unisono.card.data.domain.model;

/**
 * Estado actual de la tarjeta en su ciclo de vida
 */
public enum CardStatus {
    REQUESTED,       // Solicitud ingresada
    APPROVED,        // Aprobada (pendiente de emisión)
    ISSUED,          // Emitida físicamente
    ACTIVE,          // Activada y operativa
    BLOCKED,         // Bloqueada temporalmente
    BLOCKED_FRAUD,   // Bloqueada por fraude
    EXPIRED,         // Vencida
    CANCELLED        // Cancelada definitivamente
}
