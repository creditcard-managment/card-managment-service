package com.unisono.card.data.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Claves de cifrado de PIN (PEK), prefijo "pin-encryption"
 *
 * pin-encryption:
 *   keys:
 *     pek-v1: <Base64 de 16, 24 o 32 bytes>   # generar con: openssl rand -base64 16
 *
 * El terminal envía el id de la clave junto con el PIN block. Para rotar: agregar una clave nueva,
 * pasar los terminales a ella y después quitar la anterior. Sustituye a un HSM: en producción las
 * claves no deberían salir de uno.
 */
@Data
@Component
@ConfigurationProperties(prefix = "pin-encryption")
public class PinEncryptionProperties {

    private Map<String, String> keys = new LinkedHashMap<>();

    /**
     * Falla al arrancar si alguna clave no es Base64 de un largo AES válido
     */
    @PostConstruct
    void validate() {
        keys.forEach((id, value) -> {
            byte[] key;
            try {
                key = Base64.getDecoder().decode(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("pin-encryption.keys." + id + " no es Base64 válido");
            }
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalStateException("pin-encryption.keys." + id + " debe tener 16, 24 o 32 bytes");
            }
        });
    }
}
