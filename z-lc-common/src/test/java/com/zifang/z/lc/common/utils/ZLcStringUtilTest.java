package com.zifang.z.lc.common.utils;

import com.zifang.util.core.lang.StringUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStringUtil 单元测试.
 *
 * <p>2026-09 收口：本地 isBlank / isNotBlank 与
 * {@code StringUtil.isEmpty(String)} / {@code isNotEmpty(String)} 逐输入实测等价,
 * 本地实现已删除, 下面 6 个用例改为直接验证 z-util（注意不是 z-util 的 isBlank ——
 * 那个把纯空白也算空, 语义不同, 详见 ZLcUtilDedupEquivalenceTest）.
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
        assertThat(StringUtil.isEmpty((String) null)).isTrue();
    }

    @Test
    void shouldReturnTrueForEmptyBlank() {
        assertThat(StringUtil.isEmpty("")).isTrue();
    }

    @Test
    void shouldReturnFalseForNonEmptyBlank() {
        assertThat(StringUtil.isEmpty("hello")).isFalse();
    }

    @Test
    void shouldReturnFalseForNullNotBlank() {
        assertThat(StringUtil.isNotEmpty((String) null)).isFalse();
    }

    @Test
    void shouldReturnTrueForEmptyNotBlank() {
        assertThat(StringUtil.isNotEmpty("")).isFalse();
    }

    @Test
    void shouldReturnTrueForNonEmptyNotBlank() {
        assertThat(StringUtil.isNotEmpty("hello")).isTrue();
    }
}