package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcJsonSortUtil 单元测试
 *
 * @author zifang
 */
class ZLcJsonSortUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcJsonSortUtil> constructor = ZLcJsonSortUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldSortJsonKeys() {
        String input = "{\"c\":3,\"a\":1,\"b\":2}";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("{\"a\":1,\"b\":2,\"c\":3}");
    }

    @Test
    void shouldHandleNestedObjects() {
        String input = "{\"z\":{\"c\":3,\"a\":1},\"a\":1}";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("{\"a\":1,\"z\":{\"a\":1,\"c\":3}}");
    }

    @Test
    void shouldHandleArrays() {
        String input = "{\"a\":[3,1,2]}";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("{\"a\":[3,1,2]}");
    }

    @Test
    void shouldHandleSimpleValues() {
        String input = "\"hello\"";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("\"hello\"");
    }

    @Test
    void shouldHandleNumbers() {
        String input = "42";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("42");
    }

    @Test
    void shouldHandleEmptyObject() {
        String input = "{}";
        String result = ZLcJsonSortUtil.startSort(input);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void shouldConvertKeysToUpperCase() {
        String input = "{\"name\":\"test\",\"age\":1}";
        String result = ZLcJsonSortUtil.convertKeysToUpperCase(input);
        assertThat(result).isEqualTo("{\"Name\":\"test\",\"Age\":1}");
    }

    @Test
    void shouldHandleNestedObjectsForUpperCase() {
        String input = "{\"user\":{\"name\":\"test\"}}";
        String result = ZLcJsonSortUtil.convertKeysToUpperCase(input);
        assertThat(result).isEqualTo("{\"User\":{\"Name\":\"test\"}}");
    }

    @Test
    void shouldHandleSingleCharKeys() {
        String input = "{\"a\":1}";
        String result = ZLcJsonSortUtil.convertKeysToUpperCase(input);
        assertThat(result).isEqualTo("{\"A\":1}");
    }

    @Test
    void shouldHandleEmptyObjectForUpperCase() {
        String input = "{}";
        String result = ZLcJsonSortUtil.convertKeysToUpperCase(input);
        assertThat(result).isEqualTo("{}");
    }
}