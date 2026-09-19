package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpRequestParam 单元测试
 */
class HttpRequestParamTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        HttpRequestParam p = new HttpRequestParam();
        assertThat(p).isNotNull();
        assertThat(p.getParam()).isNull();
        assertThat(p.getDefaultValue()).isNull();
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        HttpRequestParam p = new HttpRequestParam("userId", "0");
        assertThat(p.getParam()).isEqualTo("userId");
        assertThat(p.getDefaultValue()).isEqualTo("0");
    }

    @Test
    void shouldSetAndGetParam() {
        HttpRequestParam p = new HttpRequestParam();
        p.setParam("city");
        assertThat(p.getParam()).isEqualTo("city");
    }

    @Test
    void shouldSetAndGetDefaultValue() {
        HttpRequestParam p = new HttpRequestParam();
        p.setDefaultValue("Beijing");
        assertThat(p.getDefaultValue()).isEqualTo("Beijing");
    }

    @Test
    void shouldImplementSerializable() {
        HttpRequestParam p = new HttpRequestParam();
        assertThat(p).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHandleNullValues() {
        HttpRequestParam p = new HttpRequestParam(null, null);
        assertThat(p.getParam()).isNull();
        assertThat(p.getDefaultValue()).isNull();
    }
}