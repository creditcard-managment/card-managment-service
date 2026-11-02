package com.unisono.card.shared.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración del llavero de cifrado (prefijo "encryption")
 *
 * encryption:
 *   active-key-id: k1                 # clave con la que se cifra
 *   keys:
 *     k1: <Base64 de 32 bytes>        # generar con: openssl rand -base64 32
 *   legacy-secret-key: <32 caracteres> # opcional, solo para descifrar datos sin prefijo de versión
 */
@Data
@Component
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private String activeKeyId;
    private Map<String, String> keys = new LinkedHashMap<>();
    private String legacySecretKey;
}
