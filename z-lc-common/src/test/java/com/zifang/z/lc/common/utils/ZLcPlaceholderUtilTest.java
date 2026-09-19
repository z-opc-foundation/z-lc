package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPlaceholderUtil 单元测试
 *
 * @author zifang
 */
class ZLcPlaceholderUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcPlaceholderUtil> constructor = ZLcPlaceholderUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(ZLcPlaceholderUtil.markReplace(null, new HashMap<>())).isNull();
    }

    @Test
    void shouldReturnOriginalForNullContext() {
        String result = ZLcPlaceholderUtil.markReplace("hello", null);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldReturnOriginalForEmptyContext() {
        String result = ZLcPlaceholderUtil.markReplace("hello", new HashMap<>());
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldReplacePlaceholder() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "test");

        String result = ZLcPlaceholderUtil.markReplace("hello ${name}", context);
        assertThat(result).isEqualTo("hello test");
    }

    @Test
    void shouldReplaceMultiplePlaceholders() {
        Map<String, Object> context = new HashMap<>();
        context.put("first", "Hello");
        context.put("last", "World");

        String result = ZLcPlaceholderUtil.markReplace("${first}, ${last}!", context);
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    void shouldKeepOriginalWhenKeyNotFound() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", "test");

        String result = ZLcPlaceholderUtil.markReplace("hello ${notFound}", context);
        assertThat(result).isEqualTo("hello ${notFound}");
    }

    @Test
    void shouldHandleNullValueInContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("name", null);

        String result = ZLcPlaceholderUtil.markReplace("hello ${name}", context);
        assertThat(result).isEqualTo("hello ${name}");
    }
}