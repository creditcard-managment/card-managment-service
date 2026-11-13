package com.unisono.card.shared.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Iso9564Format4PinBlockTest {

    private static final byte[] KEY = HexFormat.of().parseHex("00112233445566778899AABBCCDDEEFF");
    private static final byte[] OTHER_KEY = HexFormat.of().parseHex("FFEEDDCCBBAA99887766554433221100");
    private static final String PAN = "4532015112830366";

    @Test
    void shouldRoundTripPinsOfEveryAllowedLength() {
        for (String pin : new String[]{"1234", "00000", "987654", "123456789012"}) {
            String block = Iso9564Format4PinBlock.encrypt(pin, PAN, KEY);

            assertThat(block).hasSize(32).matches("[0-9A-F]{32}");
            assertThat(Iso9564Format4PinBlock.decrypt(block, PAN, KEY)).isEqualTo(pin);
        }
    }

    @Test
    void shouldProduceADifferentBlockEachTimeForTheSamePin() {
        String first = Iso9564Format4PinBlock.encrypt("1234", PAN, KEY);
        String second = Iso9564Format4PinBlock.encrypt("1234", PAN, KEY);

        assertThat(first).isNotEqualTo(second);
        assertThat(Iso9564Format4PinBlock.decrypt(second, PAN, KEY)).isEqualTo("1234");
    }

    @Test
    void shouldFollowTheFormat4Layout() throws Exception {
        String block = Iso9564Format4PinBlock.encrypt("1234", PAN, KEY);

        // Deshace el cifrado a mano y revisa los campos
        Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"));
        byte[] intermediate = aes.doFinal(HexFormat.of().parseHex(block));
        byte[] panField = HexFormat.of().parseHex("44532015112830366000000000000000");
        for (int i = 0; i < 16; i++) {
            intermediate[i] ^= panField[i];
        }
        String pinField = HexFormat.of().withUpperCase().formatHex(aes.doFinal(intermediate));

        assertThat(pinField.substring(0, 16)).isEqualTo("441234AAAAAAAAAA");
    }

    @Test
    void shouldRejectABlockOfAnotherCard() {
        String block = Iso9564Format4PinBlock.encrypt("1234", PAN, KEY);

        assertThatThrownBy(() -> Iso9564Format4PinBlock.decrypt(block, "4532015112830367", KEY))
                .isInstanceOf(InvalidPinBlockException.class);
    }

    @Test
    void shouldRejectABlockEncryptedWithAnotherKey() {
        String block = Iso9564Format4PinBlock.encrypt("1234", PAN, OTHER_KEY);

        assertThatThrownBy(() -> Iso9564Format4PinBlock.decrypt(block, PAN, KEY))
                .isInstanceOf(InvalidPinBlockException.class);
    }

    @Test
    void shouldRejectMalformedBlocksWithoutEchoingThem() {
        assertThatThrownBy(() -> Iso9564Format4PinBlock.decrypt("1234", PAN, KEY))
                .isInstanceOf(InvalidPinBlockException.class)
                .hasMessageNotContaining("1234");
        assertThatThrownBy(() -> Iso9564Format4PinBlock.decrypt("Z".repeat(32), PAN, KEY))
                .isInstanceOf(InvalidPinBlockException.class);
    }

    @Test
    void shouldSupportShortPansAndAes256Keys() {
        byte[] aes256 = HexFormat.of().parseHex("00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF");

        String block = Iso9564Format4PinBlock.encrypt("4321", "12345678901", aes256);

        assertThat(Iso9564Format4PinBlock.decrypt(block, "12345678901", aes256)).isEqualTo("4321");
    }

    @Test
    void shouldRejectInvalidPinsAndKeys() {
        assertThatThrownBy(() -> Iso9564Format4PinBlock.encrypt("123", PAN, KEY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Iso9564Format4PinBlock.encrypt("12a4", PAN, KEY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Iso9564Format4PinBlock.encrypt("1234", PAN, new byte[8]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
