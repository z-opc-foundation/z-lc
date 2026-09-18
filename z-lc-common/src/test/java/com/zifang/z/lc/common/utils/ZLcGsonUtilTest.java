package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcGsonUtil 单元测试
 *
 * @author zifang
 */
class ZLcGsonUtilTest {

    @Test
    void objectToJsonStr_shouldReturnEmptyObject_WhenInputIsNull() {
        String result = ZLcGsonUtil.objectToJsonStr(null);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void objectToJsonStr_shouldReturnJsonString_WhenInputIsString() {
        String result = ZLcGsonUtil.objectToJsonStr("hello");
        assertThat(result).isEqualTo("\"hello\"");
    }

    @Test
    void objectToJsonStr_shouldReturnJsonString_WhenInputIsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String result = ZLcGsonUtil.objectToJsonStr(map);
        assertThat(result).contains("\"key\"").contains("\"value\"");
    }

    @Test
    void fromJson_shouldReturnObject_WhenValidJson() {
        String result = ZLcGsonUtil.fromJson("\"hello\"", String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void toMap_shouldReturnNull_WhenJsonIsBlank() {
        Map<String, Object> result = ZLcGsonUtil.toMap(null);
        assertThat(result).isNull();
    }

    @Test
    void toMap_shouldReturnEmpty_WhenJsonIsEmpty() {
        Map<String, Object> result = ZLcGsonUtil.toMap("");
        assertThat(result).isNull();
    }

    @Test
    void toMap_shouldParseJsonToMap() {
        Map<String, Object> result = ZLcGsonUtil.toMap("{\"key\":\"value\"}");
        assertThat(result).containsEntry("key", "value");
    }

    @Test
    void toList_shouldParseJsonArray() {
        List<Map<String, Object>> result = ZLcGsonUtil.toList("[{\"a\":1},{\"b\":2}]");
        assertThat(result).hasSize(2);
    }

    @Test
    void isSame_shouldReturnTrue_WhenBothNull() {
        assertThat(ZLcGsonUtil.isSame(null, null)).isTrue();
    }

    @Test
    void isSame_shouldReturnFalse_WhenOneNull() {
        Map<String, Object> m = new HashMap<>();
        m.put("key", "value");
        assertThat(ZLcGsonUtil.isSame(m, null)).isFalse();
    }

    @Test
    void isSame_shouldReturnTrue_WhenSameContent() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Object> m2 = new HashMap<>();
        m2.put("a", 1);
        assertThat(ZLcGsonUtil.isSame(m1, m2)).isTrue();
    }

    @Test
    void isSame_shouldReturnFalse_WhenDifferentContent() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Object> m2 = new HashMap<>();
        m2.put("a", 2);
        assertThat(ZLcGsonUtil.isSame(m1, m2)).isFalse();
    }

    @Test
    void isTypeJSON_shouldReturnTrue_WhenJsonObject() {
        assertThat(ZLcGsonUtil.isTypeJSON("{\"key\":\"value\"}")).isTrue();
    }

    @Test
    void isTypeJSON_shouldReturnTrue_WhenJsonArray() {
        assertThat(ZLcGsonUtil.isTypeJSON("[1,2,3]")).isTrue();
    }

    @Test
    void isTypeJSON_shouldReturnFalse_WhenNotJson() {
        assertThat(ZLcGsonUtil.isTypeJSON("hello")).isFalse();
    }

    @Test
    void isTypeJSONObject_shouldReturnTrue_WhenValid() {
        assertThat(ZLcGsonUtil.isTypeJSONObject("{\"key\":\"value\"}")).isTrue();
    }

    @Test
    void isTypeJSONArray_shouldReturnTrue_WhenValid() {
        assertThat(ZLcGsonUtil.isTypeJSONArray("[1,2,3]")).isTrue();
    }

    @Test
    void areJsonEqual_shouldReturnTrue_WhenSameStructure() {
        assertThat(ZLcGsonUtil.areJsonEqual("{\"a\":1}", "{\"a\":1}")).isTrue();
    }

    @Test
    void areJsonEqual_shouldReturnFalse_WhenDifferentStructure() {
        assertThat(ZLcGsonUtil.areJsonEqual("{\"a\":1}", "{\"a\":2}")).isFalse();
    }

    @Test
    void areJsonEqual_shouldReturnFalse_WhenInvalidJson() {
        assertThat(ZLcGsonUtil.areJsonEqual("invalid", "json")).isFalse();
    }
}
