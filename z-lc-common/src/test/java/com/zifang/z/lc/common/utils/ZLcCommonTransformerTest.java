package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCommonTransformer 单元测试
 *
 * @author zifang
 */
class ZLcCommonTransformerTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcCommonTransformer> constructor = ZLcCommonTransformer.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldConvertListWithFunction() {
        List<String> source = Arrays.asList("a", "b", "c");
        Function<String, String> converter = s -> s.toUpperCase();
        List<String> result = ZLcCommonTransformer.convertList(source, String.class, converter);

        assertThat(result).containsExactly("A", "B", "C");
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        Function<String, String> converter = s -> s.toUpperCase();
        List<String> result = ZLcCommonTransformer.convertList(null, String.class, converter);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldConvertListWithBiConsumer() {
        List<String> source = Arrays.asList("hello", "world");
        List<TestTarget> result = ZLcCommonTransformer.convertList(source, TestTarget.class,
                (target, src) -> target.setValue(src.toUpperCase()));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getValue()).isEqualTo("HELLO");
        assertThat(result.get(1).getValue()).isEqualTo("WORLD");
    }

    @Test
    void shouldConvertPage() {
        List<String> records = Arrays.asList("a", "b");
        Function<String, String> converter = s -> s.toUpperCase();
        Map<String, Object> page = ZLcCommonTransformer.convertPage(records, 100L, 1L, 10L, converter);

        assertThat(page.get("total")).isEqualTo(100L);
        assertThat(page.get("current")).isEqualTo(1L);
        assertThat(page.get("size")).isEqualTo(10L);
        assertThat((List<String>) page.get("records")).containsExactly("A", "B");
    }

    // 测试用的内部类
    static class TestTarget {
        private String value;

        public TestTarget() {}

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}