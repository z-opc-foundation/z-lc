package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcModelDataUtil 单元测试
 *
 * @author zifang
 */
class ZLcModelDataUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcModelDataUtil> constructor = ZLcModelDataUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldPickPrimaryKeyFromIdField() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 100L);

        Long result = ZLcModelDataUtil.pickPrimaryKey(data);
        assertThat(result).isEqualTo(100L);
    }

    @Test
    void shouldPickPrimaryKeyFromIDField() {
        Map<String, Object> data = new HashMap<>();
        data.put("ID", 200L);

        Long result = ZLcModelDataUtil.pickPrimaryKey(data);
        assertThat(result).isEqualTo(200L);
    }

    @Test
    void shouldPickPrimaryKeyFromPrimaryIdField() {
        Map<String, Object> data = new HashMap<>();
        data.put("primaryId", 300L);

        Long result = ZLcModelDataUtil.pickPrimaryKey(data);
        assertThat(result).isEqualTo(300L);
    }

    @Test
    void shouldReturnNullForNullData() {
        assertThat(ZLcModelDataUtil.pickPrimaryKey(null)).isNull();
    }

    @Test
    void shouldReturnNullForEmptyData() {
        Map<String, Object> data = new HashMap<>();
        assertThat(ZLcModelDataUtil.pickPrimaryKey(data)).isNull();
    }

    @Test
    void shouldConvertLongValue() {
        assertThat(ZLcModelDataUtil.toLong(100L)).isEqualTo(100L);
    }

    @Test
    void shouldConvertIntegerToLong() {
        assertThat(ZLcModelDataUtil.toLong(100)).isEqualTo(100L);
    }

    @Test
    void shouldConvertDoubleToLong() {
        assertThat(ZLcModelDataUtil.toLong(100.5)).isEqualTo(100L);
    }

    @Test
    void shouldConvertStringToLong() {
        assertThat(ZLcModelDataUtil.toLong("100")).isEqualTo(100L);
    }

    @Test
    void shouldReturnNullForInvalidStringToLong() {
        assertThat(ZLcModelDataUtil.toLong("abc")).isNull();
    }

    @Test
    void shouldReturnNullForNullToLong() {
        assertThat(ZLcModelDataUtil.toLong(null)).isNull();
    }

    @Test
    void shouldReturnNullForUnsupportedTypeToLong() {
        assertThat(ZLcModelDataUtil.toLong(new Object())).isNull();
    }

    @Test
    void shouldConvertToString() {
        assertThat(ZLcModelDataUtil.toString(100)).isEqualTo("100");
        assertThat(ZLcModelDataUtil.toString("hello")).isEqualTo("hello");
    }

    @Test
    void shouldReturnNullForNullToString() {
        assertThat(ZLcModelDataUtil.toString(null)).isNull();
    }

    @Test
    void shouldConvertIntegerValue() {
        assertThat(ZLcModelDataUtil.toInteger(100)).isEqualTo(100);
    }

    @Test
    void shouldConvertLongToInteger() {
        assertThat(ZLcModelDataUtil.toInteger(100L)).isEqualTo(100);
    }

    @Test
    void shouldConvertStringToInteger() {
        assertThat(ZLcModelDataUtil.toInteger("100")).isEqualTo(100);
    }

    @Test
    void shouldReturnNullForInvalidStringToInteger() {
        assertThat(ZLcModelDataUtil.toInteger("abc")).isNull();
    }

    @Test
    void shouldReturnNullForNullToInteger() {
        assertThat(ZLcModelDataUtil.toInteger(null)).isNull();
    }
}