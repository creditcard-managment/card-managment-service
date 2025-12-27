package com.unisono.card.shared.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra una entidad.
 */
public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entityName, String id) {
        super(String.format("%s con id '%s' no encontrado", entityName, id));
    }
}

