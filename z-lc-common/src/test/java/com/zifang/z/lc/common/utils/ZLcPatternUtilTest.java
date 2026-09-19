package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPatternUtil 单元测试
 *
 * @author zifang
 */
class ZLcPatternUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcPatternUtil> constructor = ZLcPatternUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnFalseForNullNumeric() {
        assertThat(ZLcPatternUtil.isNumeric(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForEmptyNumeric() {
        assertThat(ZLcPatternUtil.isNumeric("")).isFalse();
    }

    @Test
    void shouldReturnTrueForPureNumbers() {
        assertThat(ZLcPatternUtil.isNumeric("123")).isTrue();
        assertThat(ZLcPatternUtil.isNumeric("0")).isTrue();
    }

    @Test
    void shouldReturnFalseForNonNumeric() {
        assertThat(ZLcPatternUtil.isNumeric("abc")).isFalse();
        assertThat(ZLcPatternUtil.isNumeric("12a")).isFalse();
        assertThat(ZLcPatternUtil.isNumeric("12.5")).isFalse();
        assertThat(ZLcPatternUtil.isNumeric("-123")).isFalse();
    }

    @Test
    void shouldReturnFalseForNullModelField() {
        assertThat(ZLcPatternUtil.isModelField(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForEmptyModelField() {
        assertThat(ZLcPatternUtil.isModelField("")).isFalse();
    }

    @Test
    void shouldReturnTrueForValidModelField() {
        assertThat(ZLcPatternUtil.isModelField("user_name")).isTrue();
        assertThat(ZLcPatternUtil.isModelField("name")).isTrue();
        assertThat(ZLcPatternUtil.isModelField("user_name_2")).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidModelField() {
        assertThat(ZLcPatternUtil.isModelField("UserName")).isFalse();
        assertThat(ZLcPatternUtil.isModelField("_user")).isFalse();
        assertThat(ZLcPatternUtil.isModelField("user-name")).isFalse();
        assertThat(ZLcPatternUtil.isModelField("1user")).isFalse();
    }
}