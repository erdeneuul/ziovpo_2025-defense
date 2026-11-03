package ru.mfa.signature;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JsonCanonicalizerTest {

    @Test
    void producesSameBytesForDifferentObjectKeyOrder() {
        byte[] first = new JsonCanonicalizer("{\"z\":2,\"a\":1}").getEncodedUTF8();
        byte[] second = new JsonCanonicalizer("{\"a\":1,\"z\":2}").getEncodedUTF8();

        assertThat(first).isEqualTo(second);
        assertThat(new String(first, StandardCharsets.UTF_8)).isEqualTo("{\"a\":1,\"z\":2}");
    }
}
