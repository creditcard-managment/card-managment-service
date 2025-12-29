package com.unisono.card.lifecycle.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Value Object que encapsula el contexto de una transición
 */
@Getter
@EqualsAndHashCode
public class TransitionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String reason;
    private final String performedBy;
    private final Map<String, Object> additionalData;

    private TransitionContext(String reason, String performedBy, Map<String, Object> additionalData) {
        this.reason = reason;
        this.performedBy = performedBy;
        this.additionalData = new HashMap<>(additionalData);
    }

    public static TransitionContext of(String reason, String performedBy) {
        return new TransitionContext(reason, performedBy, new HashMap<>());
    }

    public static TransitionContext of(String reason, String performedBy, Map<String, Object> additionalData) {
        return new TransitionContext(reason, performedBy, additionalData);
    }

    /**
     * Obtiene un dato adicional
     */
    public Object get(String key) {
        return additionalData.get(key);
    }

    /**
     * Verifica si tiene un dato adicional
     */
    public boolean has(String key) {
        return additionalData.containsKey(key);
    }

    /**
     * Crea una copia con datos adicionales
     */
    public TransitionContext withData(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(this.additionalData);
        newData.put(key, value);
        return new TransitionContext(this.reason, this.performedBy, newData);
    }

    @Override
    public String toString() {
        return String.format("TransitionContext{reason='%s', performedBy='%s', data=%s}",
                reason, performedBy, additionalData);
    }
}
