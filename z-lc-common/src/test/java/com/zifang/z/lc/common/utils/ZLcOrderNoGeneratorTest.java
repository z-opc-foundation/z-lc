package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcOrderNoGenerator 单元测试
 *
 * @author zifang
 */
class ZLcOrderNoGeneratorTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcOrderNoGenerator> constructor = ZLcOrderNoGenerator.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldGenerateWithPrefix() {
        String result = ZLcOrderNoGenerator.generate("WF-");
        assertThat(result).startsWith("WF-");
        assertThat(result.length()).isGreaterThan(3);
    }

    @Test
    void shouldGenerateWithoutPrefix() {
        String result = ZLcOrderNoGenerator.generate(null);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldGenerateEmptyPrefix() {
        String result = ZLcOrderNoGenerator.generate("");
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldGenerateUniqueNumbers() {
        String result1 = ZLcOrderNoGenerator.generate("ID-");
        String result2 = ZLcOrderNoGenerator.generate("ID-");
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void shouldHandleLongPrefix() {
        String result = ZLcOrderNoGenerator.generate("VERY-LONG-PREFIX-");
        assertThat(result).startsWith("VERY-LONG-PREFIX-");
    }
}