package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStringUtil 单元测试
 *
 * @author zifang
 */
class ZLcStringUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcStringUtil> constructor = ZLcStringUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullUnderlineToCamel() {
        assertThat(ZLcStringUtil.underlineToCamel(null, false)).isNull();
    }

    @Test
    void shouldReturnEmptyForEmptyUnderlineToCamel() {
        assertThat(ZLcStringUtil.underlineToCamel("", false)).isEmpty();
    }

    @Test
    void shouldReturnOriginalWhenNoUnderscore() {
        assertThat(ZLcStringUtil.underlineToCamel("userName", false)).isEqualTo("userName");
    }

    @Test
    void shouldConvertUnderlineToCamelLower() {
        assertThat(ZLcStringUtil.underlineToCamel("user_name", false)).isEqualTo("userName");
    }

    @Test
    void shouldConvertUnderlineToCamelUpper() {
        assertThat(ZLcStringUtil.underlineToCamel("user_name", true)).isEqualTo("UserName");
    }

    @Test
    void shouldHandleMultipleUnderscores() {
        assertThat(ZLcStringUtil.underlineToCamel("user_first_name", false)).isEqualTo("userFirstName");
    }

    @Test
    void shouldReturnNullForNullCamelToUnderline() {
        assertThat(ZLcStringUtil.camelToUnderline(null)).isNull();
    }

    @Test
    void shouldReturnEmptyForEmptyCamelToUnderline() {
        assertThat(ZLcStringUtil.camelToUnderline("")).isEmpty();
    }

    @Test
    void shouldConvertCamelToUnderline() {
        assertThat(ZLcStringUtil.camelToUnderline("userName")).isEqualTo("user_name");
    }

    @Test
    void shouldHandleConsecutiveUpperCase() {
        assertThat(ZLcStringUtil.camelToUnderline("userFirstName")).isEqualTo("user_first_name");
    }

    @Test
    void shouldHandleSingleWord() {
        assertThat(ZLcStringUtil.camelToUnderline("name")).isEqualTo("name");
    }

    @Test
    void shouldTrimStringValue() {
        Object result = ZLcStringUtil.trim("  hello  ");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void shouldReturnOriginalForNonStringValue() {
        Object obj = 123;
        Object result = ZLcStringUtil.trim(obj);
        assertThat(result).isEqualTo(obj);
    }

    @Test
    void shouldReturnTrueForNullBlank() {
        assertThat(ZLcStringUtil.isBlank(null)).isTrue();
    }

    @Test
    void shouldReturnTrueForEmptyBlank() {
        assertThat(ZLcStringUtil.isBlank("")).isTrue();
    }

    @Test
    void shouldReturnFalseForNonEmptyBlank() {
        assertThat(ZLcStringUtil.isBlank("hello")).isFalse();
    }

    @Test
    void shouldReturnFalseForNullNotBlank() {
        assertThat(ZLcStringUtil.isNotBlank(null)).isFalse();
    }

    @Test
    void shouldReturnTrueForEmptyNotBlank() {
        assertThat(ZLcStringUtil.isNotBlank("")).isFalse();
    }

    @Test
    void shouldReturnTrueForNonEmptyNotBlank() {
        assertThat(ZLcStringUtil.isNotBlank("hello")).isTrue();
    }
}