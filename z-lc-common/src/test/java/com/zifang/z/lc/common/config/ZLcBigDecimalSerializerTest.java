package com.zifang.z.lc.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBigDecimalSerializer 单元测试
 *
 * @author zifang
 */
class ZLcBigDecimalSerializerTest {

    private ZLcBigDecimalSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new ZLcBigDecimalSerializer();
    }

    @Test
    void serialize_shouldFormatWithTwoDecimals() throws Exception {
        BigDecimal value = new BigDecimal("123.456");
        String result = serialize(value);
        assertThat(result).isEqualTo("123.46");
    }

    @Test
    void serialize_shouldRoundHalfUp() throws Exception {
        BigDecimal value = new BigDecimal("123.455");
        String result = serialize(value);
        assertThat(result).isEqualTo("123.46");
    }

    @Test
    void serialize_shouldHandleZero() throws Exception {
        BigDecimal value = BigDecimal.ZERO;
        String result = serialize(value);
        assertThat(result).isEqualTo("0.00");
    }

    @Test
    void serialize_shouldHandleInteger() throws Exception {
        BigDecimal value = new BigDecimal("100");
        String result = serialize(value);
        assertThat(result).isEqualTo("100.00");
    }

    @Test
    void serialize_shouldHandleNegativeNumber() throws Exception {
        BigDecimal value = new BigDecimal("-42.5");
        String result = serialize(value);
        assertThat(result).isEqualTo("-42.50");
    }

    @Test
    void serialize_shouldHandleLargeNumber() throws Exception {
        BigDecimal value = new BigDecimal("1234567.891");
        String result = serialize(value);
        assertThat(result).isEqualTo("1234567.89");
    }

    @Test
    void serialize_shouldHandleSmallNumber() throws Exception {
        BigDecimal value = new BigDecimal("0.001");
        String result = serialize(value);
        assertThat(result).isEqualTo("0.00");
    }

    private String serialize(BigDecimal value) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, serializer);
        mapper.registerModule(module);

        // 包装为一个简单的对象来测试序列化
        TestWrapper wrapper = new TestWrapper(value);
        String json = mapper.writeValueAsString(wrapper);
        // 提取 value 字段的值
        return json.substring(json.indexOf(":") + 1, json.indexOf("}")).trim().replace("\"", "");
    }

    private static class TestWrapper {
        public BigDecimal value;

        TestWrapper(BigDecimal value) {
            this.value = value;
        }
    }
}
