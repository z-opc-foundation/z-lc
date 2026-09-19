package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RemoteServiceTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class RemoteServiceTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetServiceCode() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setServiceCode("service-001");
        assertThat(attachment.getServiceCode()).isEqualTo("service-001");
    }

    @Test
    void shouldSetAndGetServiceName() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setServiceName("服务名称");
        assertThat(attachment.getServiceName()).isEqualTo("服务名称");
    }

    @Test
    void shouldSetAndGetServiceDesc() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setServiceDesc("服务描述");
        assertThat(attachment.getServiceDesc()).isEqualTo("服务描述");
    }

    @Test
    void shouldSetAndGetServiceType() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setServiceType("HTTP");
        assertThat(attachment.getServiceType()).isEqualTo("HTTP");
    }

    @Test
    void shouldSetAndGetRequestParams() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        HttpRequestParam param1 = new HttpRequestParam("param1", "value1");
        HttpRequestParam param2 = new HttpRequestParam("param2", "value2");
        
        List<HttpRequestParam> requestParams = Arrays.asList(param1, param2);
        attachment.setRequestParams(requestParams);
        
        assertThat(attachment.getRequestParams()).hasSize(2);
        assertThat(attachment.getRequestParams().get(0).getParam()).isEqualTo("param1");
        assertThat(attachment.getRequestParams().get(1).getParam()).isEqualTo("param2");
    }

    @Test
    void shouldSetAndGetHttpInterfaceConfig() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        HttpInterfaceConfig httpInterfaceConfig = new HttpInterfaceConfig();
        httpInterfaceConfig.setRequestUrl("https://api.example.com");
        
        attachment.setHttpInterfaceConfig(httpInterfaceConfig);
        
        assertThat(attachment.getHttpInterfaceConfig()).isEqualTo(httpInterfaceConfig);
        assertThat(attachment.getHttpInterfaceConfig().getRequestUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    void shouldSetAndGetOpenPlatformInterfaceConfig() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        OpenPlatformInterfaceConfig openPlatformInterfaceConfig = new OpenPlatformInterfaceConfig();
        openPlatformInterfaceConfig.setAppId(123L);
        
        attachment.setOpenPlatformInterfaceConfig(openPlatformInterfaceConfig);
        
        assertThat(attachment.getOpenPlatformInterfaceConfig()).isEqualTo(openPlatformInterfaceConfig);
        assertThat(attachment.getOpenPlatformInterfaceConfig().getAppId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getServiceCode()).isNull();
        assertThat(attachment.getServiceName()).isNull();
        assertThat(attachment.getServiceDesc()).isNull();
        assertThat(attachment.getServiceType()).isNull();
        assertThat(attachment.getRequestParams()).isNull();
        assertThat(attachment.getHttpInterfaceConfig()).isNull();
        assertThat(attachment.getOpenPlatformInterfaceConfig()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setServiceCode("");
        attachment.setServiceName("");
        attachment.setServiceDesc("");
        attachment.setServiceType("");
        attachment.setTreeNodeId("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getServiceCode()).isEmpty();
        assertThat(attachment.getServiceName()).isEmpty();
        assertThat(attachment.getServiceDesc()).isEmpty();
        assertThat(attachment.getServiceType()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setServiceCode(null);
        attachment.setServiceName(null);
        attachment.setServiceDesc(null);
        attachment.setServiceType(null);
        attachment.setTreeNodeId(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getServiceCode()).isNull();
        assertThat(attachment.getServiceName()).isNull();
        assertThat(attachment.getServiceDesc()).isNull();
        assertThat(attachment.getServiceType()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldSetEmptyRequestParams() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setRequestParams(Arrays.asList());
        assertThat(attachment.getRequestParams()).isEmpty();
    }

    @Test
    void shouldSetNullRequestParams() {
        RemoteServiceTreeNodeAttachment attachment = new RemoteServiceTreeNodeAttachment();
        attachment.setRequestParams(null);
        assertThat(attachment.getRequestParams()).isNull();
    }
}