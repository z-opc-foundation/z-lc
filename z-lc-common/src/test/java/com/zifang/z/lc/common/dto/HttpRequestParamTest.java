package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpRequestParam 单元测试
 *
 * @author zifang
 */
class HttpRequestParamTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        assertThat(param).isNotNull();
    }

    @Test
    void shouldSetAndGetParam() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        param.setParam("testParam");
        assertThat(param.getParam()).isEqualTo("testParam");
    }

    @Test
    void shouldSetAndGetDefaultValue() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        param.setDefaultValue("testValue");
        assertThat(param.getDefaultValue()).isEqualTo("testValue");
    }

    @Test
    void shouldHandleNullValues() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        assertThat(param.getParam()).isNull();
        assertThat(param.getDefaultValue()).isNull();
    }

    @Test
    void shouldSetEmptyStrings() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        param.setParam("");
        param.setDefaultValue("");
        
        assertThat(param.getParam()).isEmpty();
        assertThat(param.getDefaultValue()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        param.setParam(null);
        param.setDefaultValue(null);
        
        assertThat(param.getParam()).isNull();
        assertThat(param.getDefaultValue()).isNull();
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam("param1", "default1");
        assertThat(param.getParam()).isEqualTo("param1");
        assertThat(param.getDefaultValue()).isEqualTo("default1");
    }

    @Test
    void shouldImplementSerializable() {
        com.zifang.z.lc.common.dto.tree.HttpRequestParam param = new com.zifang.z.lc.common.dto.tree.HttpRequestParam();
        assertThat(param).isInstanceOf(java.io.Serializable.class);
    }
}
