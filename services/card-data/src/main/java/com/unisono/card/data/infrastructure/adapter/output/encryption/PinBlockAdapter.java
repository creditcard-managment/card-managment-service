package com.unisono.card.data.infrastructure.adapter.output.encryption;

import com.unisono.card.data.domain.port.output.PinBlockPort;
import com.unisono.card.data.infrastructure.config.PinEncryptionProperties;
import com.unisono.card.shared.domain.valueobject.CardNumber;
import com.unisono.card.shared.infrastructure.security.InvalidPinBlockException;
import com.unisono.card.shared.infrastructure.security.Iso9564Format4PinBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

/**
 * Descifra PIN blocks ISO 9564 formato 4 con las claves de {@link PinEncryptionProperties}
 */
@Component
@RequiredArgsConstructor
public class PinBlockAdapter implements PinBlockPort {

    private final PinEncryptionProperties properties;

    @Override
    public String decryptPin(String keyId, String pinBlock, CardNumber pan) {
        String encodedKey = keyId != null ? properties.getKeys().get(keyId) : null;
        if (encodedKey == null) {
            throw new InvalidPinBlockException("Clave de cifrado de PIN desconocida: " + keyId);
        }
        byte[] key = Base64.getDecoder().decode(encodedKey);
        try {
            return Iso9564Format4PinBlock.decrypt(pinBlock, pan.getValue(), key);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }
}
