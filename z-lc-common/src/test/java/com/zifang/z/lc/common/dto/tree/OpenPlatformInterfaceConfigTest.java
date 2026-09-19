package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenPlatformInterfaceConfig 单元测试
 *
 * @author zifang
 */
class OpenPlatformInterfaceConfigTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldSetAndGetAppId() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setAppId(123L);
        assertThat(config.getAppId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetApiId() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setApiId(456L);
        assertThat(config.getApiId()).isEqualTo(456L);
    }

    @Test
    void shouldSetAndGetRequestUrl() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestUrl("https://open-platform.example.com");
        assertThat(config.getRequestUrl()).isEqualTo("https://open-platform.example.com");
    }

    @Test
    void shouldSetAndGetRequestType() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestType("POST");
        assertThat(config.getRequestType()).isEqualTo("POST");
    }

    @Test
    void shouldSetAndGetRequestBody() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestBody("{\"key\":\"value\"}");
        assertThat(config.getRequestBody()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void shouldSetAndGetRequestParamList() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        OpenPlatformInterfaceConfig.OpenRequestParam param1 = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param1.setOpenParamsName("param1");
        OpenPlatformInterfaceConfig.OpenRequestParam param2 = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param2.setOpenParamsName("param2");
        
        List<OpenPlatformInterfaceConfig.OpenRequestParam> paramList = Arrays.asList(param1, param2);
        config.setRequestParamList(paramList);
        
        assertThat(config.getRequestParamList()).hasSize(2);
        assertThat(config.getRequestParamList().get(0).getOpenParamsName()).isEqualTo("param1");
        assertThat(config.getRequestParamList().get(1).getOpenParamsName()).isEqualTo("param2");
    }

    @Test
    void shouldHandleNullValues() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        assertThat(config.getAppId()).isNull();
        assertThat(config.getApiId()).isNull();
        assertThat(config.getRequestUrl()).isNull();
        assertThat(config.getRequestType()).isNull();
        assertThat(config.getRequestBody()).isNull();
        assertThat(config.getRequestParamList()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        assertThat(config).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestUrl("");
        config.setRequestType("");
        config.setRequestBody("");
        
        assertThat(config.getRequestUrl()).isEmpty();
        assertThat(config.getRequestType()).isEmpty();
        assertThat(config.getRequestBody()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestUrl(null);
        config.setRequestType(null);
        config.setRequestBody(null);
        
        assertThat(config.getRequestUrl()).isNull();
        assertThat(config.getRequestType()).isNull();
        assertThat(config.getRequestBody()).isNull();
    }

    @Test
    void shouldSetEmptyRequestParamList() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestParamList(Arrays.asList());
        assertThat(config.getRequestParamList()).isEmpty();
    }

    @Test
    void shouldSetNullRequestParamList() {
        OpenPlatformInterfaceConfig config = new OpenPlatformInterfaceConfig();
        config.setRequestParamList(null);
        assertThat(config.getRequestParamList()).isNull();
    }

    @Test
    void shouldCreateOpenRequestParamWithDefaultConstructor() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        assertThat(param).isNotNull();
    }

    @Test
    void shouldSetAndGetOpenRequestParamId() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setId(123L);
        assertThat(param.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetOpenRequestParamIndex() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setIndex(1);
        assertThat(param.getIndex()).isEqualTo(1);
    }

    @Test
    void shouldSetAndGetOpenParamsName() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setOpenParamsName("openParam1");
        assertThat(param.getOpenParamsName()).isEqualTo("openParam1");
    }

    @Test
    void shouldSetAndGetInsertParamsName() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setInsertParamsName("insertParam1");
        assertThat(param.getInsertParamsName()).isEqualTo("insertParam1");
    }

    @Test
    void shouldSetAndGetParamsType() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setParamsType("string");
        assertThat(param.getParamsType()).isEqualTo("string");
    }

    @Test
    void shouldSetAndGetExpandType() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setExpandType("text");
        assertThat(param.getExpandType()).isEqualTo("text");
    }

    @Test
    void shouldSetAndGetMustWrite() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setMustWrite(true);
        assertThat(param.getMustWrite()).isTrue();
    }

    @Test
    void shouldSetAndGetExampleValue() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setExampleValue("example");
        assertThat(param.getExampleValue()).isEqualTo("example");
    }

    @Test
    void shouldSetAndGetDescription() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setDescription("描述");
        assertThat(param.getDescription()).isEqualTo("描述");
    }

    @Test
    void shouldSetAndGetIsEdit() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setIsEdit(true);
        assertThat(param.getIsEdit()).isTrue();
    }

    @Test
    void shouldSetAndGetIsAdd() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setIsAdd(true);
        assertThat(param.getIsAdd()).isTrue();
    }

    @Test
    void shouldSetAndGetIsCanOperate() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setIsCanOperate(true);
        assertThat(param.getIsCanOperate()).isTrue();
    }

    @Test
    void shouldSetAndGetChildNode() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        OpenPlatformInterfaceConfig.OpenRequestParam child = new OpenPlatformInterfaceConfig.OpenRequestParam();
        child.setOpenParamsName("childParam");
        
        List<OpenPlatformInterfaceConfig.OpenRequestParam> childNode = Arrays.asList(child);
        param.setChildNode(childNode);
        
        assertThat(param.getChildNode()).hasSize(1);
        assertThat(param.getChildNode().get(0).getOpenParamsName()).isEqualTo("childParam");
    }

    @Test
    void shouldHandleNullValuesForOpenRequestParam() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        assertThat(param.getId()).isNull();
        assertThat(param.getIndex()).isNull();
        assertThat(param.getOpenParamsName()).isNull();
        assertThat(param.getInsertParamsName()).isNull();
        assertThat(param.getParamsType()).isNull();
        assertThat(param.getExpandType()).isNull();
        assertThat(param.getMustWrite()).isNull();
        assertThat(param.getExampleValue()).isNull();
        assertThat(param.getDescription()).isNull();
        assertThat(param.getIsEdit()).isNull();
        assertThat(param.getIsAdd()).isNull();
        assertThat(param.getIsCanOperate()).isNull();
        assertThat(param.getChildNode()).isNull();
    }

    @Test
    void shouldImplementSerializableForOpenRequestParam() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        assertThat(param).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStringsForOpenRequestParam() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setOpenParamsName("");
        param.setInsertParamsName("");
        param.setParamsType("");
        param.setExpandType("");
        param.setExampleValue("");
        param.setDescription("");
        
        assertThat(param.getOpenParamsName()).isEmpty();
        assertThat(param.getInsertParamsName()).isEmpty();
        assertThat(param.getParamsType()).isEmpty();
        assertThat(param.getExpandType()).isEmpty();
        assertThat(param.getExampleValue()).isEmpty();
        assertThat(param.getDescription()).isEmpty();
    }

    @Test
    void shouldSetNullStringsForOpenRequestParam() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setOpenParamsName(null);
        param.setInsertParamsName(null);
        param.setParamsType(null);
        param.setExpandType(null);
        param.setExampleValue(null);
        param.setDescription(null);
        
        assertThat(param.getOpenParamsName()).isNull();
        assertThat(param.getInsertParamsName()).isNull();
        assertThat(param.getParamsType()).isNull();
        assertThat(param.getExpandType()).isNull();
        assertThat(param.getExampleValue()).isNull();
        assertThat(param.getDescription()).isNull();
    }

    @Test
    void shouldSetEmptyChildNode() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setChildNode(Arrays.asList());
        assertThat(param.getChildNode()).isEmpty();
    }

    @Test
    void shouldSetNullChildNode() {
        OpenPlatformInterfaceConfig.OpenRequestParam param = new OpenPlatformInterfaceConfig.OpenRequestParam();
        param.setChildNode(null);
        assertThat(param.getChildNode()).isNull();
    }
}