package com.unisono.card.shared.infrastructure.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * PIN block ISO 9564-1 formato 4 (AES).
 * <p>
 * El terminal cifra el PIN con una clave de cifrado de PIN (PEK) que comparte con quien lo verifica.
 * Los sistemas intermedios solo reenvían el bloque: no pueden leer el PIN. El bloque queda atado al PAN,
 * así que no sirve para otra tarjeta.
 * <pre>
 * Campo PIN (16 bytes): 4 | N | N dígitos del PIN | relleno 0xA hasta el nibble 15 | 8 bytes aleatorios
 * Campo PAN (16 bytes): M = largo del PAN - 12 | PAN (12 a 19 dígitos) | ceros
 * Bloque = AES(K, AES(K, campo PIN) XOR campo PAN)
 * </pre>
 * Descifrar hace el camino inverso y valida la estructura del campo PIN: con una clave equivocada o el PAN
 * de otra tarjeta, la estructura no cierra y se rechaza con {@link InvalidPinBlockException}.
 */
public final class Iso9564Format4PinBlock {

    public static final int BLOCK_HEX_LENGTH = 32;

    private static final int BLOCK_SIZE = 16;
    private static final int FORMAT = 4;
    private static final int FILL = 0xA;
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 12;
    private static final HexFormat HEX = HexFormat.of().withUpperCase();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Iso9564Format4PinBlock() {
    }

    /**
     * Cifra un PIN. Lo usan los terminales (y los tests): los servicios solo descifran.
     *
     * @param key clave AES de 16, 24 o 32 bytes
     * @return el bloque en hexadecimal (32 caracteres)
     */
    public static String encrypt(String pin, String pan, byte[] key) {
        validatePin(pin);
        byte[] pinField = pinField(pin);
        byte[] panField = panField(pan);
        try {
            byte[] intermediate = aes(Cipher.ENCRYPT_MODE, key, pinField);
            xor(intermediate, panField);
            return HEX.formatHex(aes(Cipher.ENCRYPT_MODE, key, intermediate));
        } finally {
            Arrays.fill(pinField, (byte) 0);
        }
    }

    /**
     * Descifra un bloque y devuelve el PIN
     *
     * @throws InvalidPinBlockException si el bloque no tiene el formato esperado, o la clave o el PAN no corresponden
     */
    public static String decrypt(String pinBlockHex, String pan, byte[] key) {
        byte[] block = parseBlock(pinBlockHex);
        byte[] panField = panField(pan);
        byte[] pinField = null;
        try {
            byte[] intermediate = aes(Cipher.DECRYPT_MODE, key, block);
            xor(intermediate, panField);
            pinField = aes(Cipher.DECRYPT_MODE, key, intermediate);
            return readPin(pinField);
        } finally {
            if (pinField != null) {
                Arrays.fill(pinField, (byte) 0);
            }
        }
    }

    private static byte[] pinField(String pin) {
        int[] nibbles = new int[BLOCK_SIZE * 2];
        nibbles[0] = FORMAT;
        nibbles[1] = pin.length();
        for (int i = 0; i < pin.length(); i++) {
            nibbles[2 + i] = pin.charAt(i) - '0';
        }
        for (int i = 2 + pin.length(); i < BLOCK_SIZE; i++) {
            nibbles[i] = FILL;
        }
        for (int i = BLOCK_SIZE; i < nibbles.length; i++) {
            nibbles[i] = RANDOM.nextInt(16);
        }
        byte[] field = toBytes(nibbles);
        Arrays.fill(nibbles, 0);
        return field;
    }

    private static String readPin(byte[] pinField) {
        int[] nibbles = toNibbles(pinField);
        try {
            int length = nibbles[1];
            if (nibbles[0] != FORMAT || length < MIN_PIN_LENGTH || length > MAX_PIN_LENGTH) {
                throw new InvalidPinBlockException("PIN block inválido");
            }
            StringBuilder pin = new StringBuilder(length);
            for (int i = 2; i < 2 + length; i++) {
                if (nibbles[i] > 9) {
                    throw new InvalidPinBlockException("PIN block inválido");
                }
                pin.append((char) ('0' + nibbles[i]));
            }
            for (int i = 2 + length; i < BLOCK_SIZE; i++) {
                if (nibbles[i] != FILL) {
                    throw new InvalidPinBlockException("PIN block inválido");
                }
            }
            return pin.toString();
        } finally {
            Arrays.fill(nibbles, 0);
        }
    }

    private static byte[] panField(String pan) {
        if (pan == null || !pan.matches("\\d{1,19}")) {
            throw new IllegalArgumentException("El PAN debe tener entre 1 y 19 dígitos");
        }
        String digits = pan.length() < 12 ? "0".repeat(12 - pan.length()) + pan : pan;
        int[] nibbles = new int[BLOCK_SIZE * 2];
        nibbles[0] = digits.length() - 12;
        for (int i = 0; i < digits.length(); i++) {
            nibbles[1 + i] = digits.charAt(i) - '0';
        }
        return toBytes(nibbles);
    }

    private static void validatePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH || !pin.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("El PIN debe tener entre 4 y 12 dígitos");
        }
    }

    private static byte[] parseBlock(String pinBlockHex) {
        if (pinBlockHex == null || pinBlockHex.length() != BLOCK_HEX_LENGTH) {
            throw new InvalidPinBlockException("El PIN block debe tener 32 caracteres hexadecimales");
        }
        try {
            return HEX.parseHex(pinBlockHex);
        } catch (IllegalArgumentException e) {
            throw new InvalidPinBlockException("El PIN block debe tener 32 caracteres hexadecimales");
        }
    }

    private static byte[] aes(int mode, byte[] key, byte[] input) {
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
            throw new IllegalArgumentException("La clave de PIN debe ser AES de 16, 24 o 32 bytes");
        }
        try {
            // Un solo bloque de 16 bytes por paso, como define el formato 4 (sin padding ni IV)
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo procesar el PIN block", e);
        }
    }

    private static void xor(byte[] target, byte[] mask) {
        for (int i = 0; i < target.length; i++) {
            target[i] ^= mask[i];
        }
    }

    private static byte[] toBytes(int[] nibbles) {
        byte[] bytes = new byte[nibbles.length / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((nibbles[2 * i] << 4) | nibbles[2 * i + 1]);
        }
        return bytes;
    }

    private static int[] toNibbles(byte[] bytes) {
        int[] nibbles = new int[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            nibbles[2 * i] = (bytes[i] >> 4) & 0xF;
            nibbles[2 * i + 1] = bytes[i] & 0xF;
        }
        return nibbles;
    }
}
