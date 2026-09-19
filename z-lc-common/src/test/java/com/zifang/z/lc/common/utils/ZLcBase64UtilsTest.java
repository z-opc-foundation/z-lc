package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBase64Utils 单元测试
 *
 * @author zifang
 */
class ZLcBase64UtilsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcBase64Utils> constructor = ZLcBase64Utils.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldEncodeBytes() {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String encoded = ZLcBase64Utils.encode(data);
        assertThat(encoded).isEqualTo("SGVsbG8sIFdvcmxkIQ==");
    }

    @Test
    void shouldReturnNullForNullBytes() {
        assertThat(ZLcBase64Utils.encode((byte[]) null)).isNull();
    }

    @Test
    void shouldEncodeString() {
        String encoded = ZLcBase64Utils.encode("Hello, World!");
        assertThat(encoded).isEqualTo("SGVsbG8sIFdvcmxkIQ==");
    }

    @Test
    void shouldReturnNullForNullString() {
        assertThat(ZLcBase64Utils.encode((String) null)).isNull();
    }

    @Test
    void shouldDecodeString() {
        byte[] decoded = ZLcBase64Utils.decode("SGVsbG8sIFdvcmxkIQ==");
        assertThat(decoded).isEqualTo("Hello, World!".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReturnNullForNullDecode() {
        assertThat(ZLcBase64Utils.decode(null)).isNull();
    }

    @Test
    void shouldDecodeToString() {
        String decoded = ZLcBase64Utils.decodeToString("SGVsbG8sIFdvcmxkIQ==");
        assertThat(decoded).isEqualTo("Hello, World!");
    }

    @Test
    void shouldReturnNullForNullDecodeToString() {
        assertThat(ZLcBase64Utils.decodeToString(null)).isNull();
    }

    @Test
    void shouldEncodeUrlSafe() {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String encoded = ZLcBase64Utils.encodeUrlSafe(data);
        assertThat(encoded).isEqualTo("SGVsbG8sIFdvcmxkIQ");
    }

    @Test
    void shouldReturnNullForNullUrlSafeEncode() {
        assertThat(ZLcBase64Utils.encodeUrlSafe(null)).isNull();
    }

    @Test
    void shouldDecodeUrlSafe() {
        byte[] decoded = ZLcBase64Utils.decodeUrlSafe("SGVsbG8sIFdvcmxkIQ");
        assertThat(decoded).isEqualTo("Hello, World!".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReturnNullForNullUrlSafeDecode() {
        assertThat(ZLcBase64Utils.decodeUrlSafe(null)).isNull();
    }

    @Test
    void shouldEncodeAndDecodeChineseCharacters() {
        String original = "你好，世界！";
        String encoded = ZLcBase64Utils.encode(original);
        String decoded = ZLcBase64Utils.decodeToString(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void shouldEncodeEmptyString() {
        String encoded = ZLcBase64Utils.encode("");
        assertThat(encoded).isEqualTo("");
    }

    @Test
    void shouldEncodeEmptyBytes() {
        String encoded = ZLcBase64Utils.encode(new byte[0]);
        assertThat(encoded).isEqualTo("");
    }
}
