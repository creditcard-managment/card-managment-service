package com.unisono.card.data.domain.port.output;

import com.unisono.card.shared.domain.valueobject.CardNumber;

/**
 * Port de salida: descifrado de PIN blocks (ISO 9564 formato 4)
 *
 * El PIN se descifra solo en memoria, para compararlo contra el hash. Nunca se guarda ni se loguea.
 */
public interface PinBlockPort {

    /**
     * @param keyId    id de la clave de cifrado de PIN con la que el terminal cifró el bloque
     * @param pinBlock bloque en hexadecimal (32 caracteres)
     * @param pan      PAN de la tarjeta: el bloque está atado a él
     * @return el PIN en claro
     * @throws com.unisono.card.shared.infrastructure.security.InvalidPinBlockException si la clave no existe
     *         o el bloque no se puede descifrar (formato, clave o tarjeta equivocados)
     */
    String decryptPin(String keyId, String pinBlock, CardNumber pan);
}
