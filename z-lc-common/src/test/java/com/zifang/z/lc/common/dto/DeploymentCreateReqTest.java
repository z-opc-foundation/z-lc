package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeploymentCreateReq 单元测试
 *
 * @author zifang
 */
class DeploymentCreateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode("app-001");
        assertThat(req.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetMaterializationId() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setMaterializationId(123L);
        assertThat(req.getMaterializationId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetDeployType() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setDeployType("HOT_LOAD");
        assertThat(req.getDeployType()).isEqualTo("HOT_LOAD");
    }

    @Test
    void shouldSetAndGetVersion() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setVersion("1.0.0");
        assertThat(req.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldSetAndGetTenantCode() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setTenantCode("tenant-001");
        assertThat(req.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldHandleNullValues() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getMaterializationId()).isNull();
        assertThat(req.getDeployType()).isNull();
        assertThat(req.getVersion()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode("");
        req.setDeployType("");
        req.setVersion("");
        req.setTenantCode("");
        
        assertThat(req.getAppCode()).isEmpty();
        assertThat(req.getDeployType()).isEmpty();
        assertThat(req.getVersion()).isEmpty();
        assertThat(req.getTenantCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode(null);
        req.setDeployType(null);
        req.setVersion(null);
        req.setTenantCode(null);
        
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getDeployType()).isNull();
        assertThat(req.getVersion()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }
}