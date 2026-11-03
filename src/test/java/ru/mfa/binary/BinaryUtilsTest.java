package ru.mfa.binary;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryUtilsTest {

    @Test
    void writesUnsignedValuesInBigEndianOrder() {
        assertThat(BinaryUtils.writeU16BE(0xABCD)).containsExactly(0xAB, 0xCD);
        assertThat(BinaryUtils.writeU32BE(0x01020304L)).containsExactly(1, 2, 3, 4);
        assertThat(BinaryUtils.writeU64BE(0x0102030405060708L))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void writesLengthPrefixedUtf8String() {
        byte[] encoded = BinaryUtils.writeString("тест");
        ByteBuffer buffer = ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN);
        int length = buffer.getInt();
        byte[] value = new byte[length];
        buffer.get(value);

        assertThat(length).isEqualTo(8);
        assertThat(new String(value, StandardCharsets.UTF_8)).isEqualTo("тест");
    }

    @Test
    void writesUuidAsTwoBigEndianLongs() {
        UUID uuid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
        assertThat(BinaryUtils.writeUUID(uuid)).containsExactly(
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
                0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF);
    }
}
