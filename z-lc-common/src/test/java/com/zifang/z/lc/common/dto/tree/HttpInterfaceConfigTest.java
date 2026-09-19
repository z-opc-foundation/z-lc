package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpInterfaceConfig 单元测试
 *
 * @author zifang
 */
class HttpInterfaceConfigTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldSetAndGetRequestUrl() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestUrl("https://api.example.com");
        assertThat(config.getRequestUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    void shouldSetAndGetRequestType() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestType("POST");
        assertThat(config.getRequestType()).isEqualTo("POST");
    }

    @Test
    void shouldSetAndGetRequestBody() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestBody("{\"key\":\"{{paramName}}\"}");
        assertThat(config.getRequestBody()).isEqualTo("{\"key\":\"{{paramName}}\"}");
    }

    @Test
    void shouldSetAndGetRequestParamList() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        HttpRequestParam param1 = new HttpRequestParam("param1", "value1");
        HttpRequestParam param2 = new HttpRequestParam("param2", "value2");
        
        List<HttpRequestParam> paramList = Arrays.asList(param1, param2);
        config.setRequestParamList(paramList);
        
        assertThat(config.getRequestParamList()).hasSize(2);
        assertThat(config.getRequestParamList().get(0).getParam()).isEqualTo("param1");
        assertThat(config.getRequestParamList().get(1).getParam()).isEqualTo("param2");
    }

    @Test
    void shouldHandleNullValues() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        assertThat(config.getRequestUrl()).isNull();
        assertThat(config.getRequestType()).isNull();
        assertThat(config.getRequestBody()).isNull();
        assertThat(config.getRequestParamList()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        assertThat(config).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestUrl("");
        config.setRequestType("");
        config.setRequestBody("");
        
        assertThat(config.getRequestUrl()).isEmpty();
        assertThat(config.getRequestType()).isEmpty();
        assertThat(config.getRequestBody()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestUrl(null);
        config.setRequestType(null);
        config.setRequestBody(null);
        
        assertThat(config.getRequestUrl()).isNull();
        assertThat(config.getRequestType()).isNull();
        assertThat(config.getRequestBody()).isNull();
    }

    @Test
    void shouldSetEmptyRequestParamList() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestParamList(Arrays.asList());
        assertThat(config.getRequestParamList()).isEmpty();
    }

    @Test
    void shouldSetNullRequestParamList() {
        HttpInterfaceConfig config = new HttpInterfaceConfig();
        config.setRequestParamList(null);
        assertThat(config.getRequestParamList()).isNull();
    }
}