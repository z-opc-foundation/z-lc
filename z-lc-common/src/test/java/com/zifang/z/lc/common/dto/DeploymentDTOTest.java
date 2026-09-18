package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeploymentDTO 单元测试
 *
 * @author zifang
 */
class DeploymentDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DeploymentDTO dto = new DeploymentDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetDeploymentId() {
        DeploymentDTO dto = new DeploymentDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetAppCode() {
        DeploymentDTO dto = new DeploymentDTO();
        dto.setAppCode("test_app");
        assertThat(dto.getAppCode()).isEqualTo("test_app");
    }

    @Test
    void shouldSetAndGetVersion() {
        DeploymentDTO dto = new DeploymentDTO();
        dto.setVersion("1.0.0");
        assertThat(dto.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldSetAndGetStatus() {
        DeploymentDTO dto = new DeploymentDTO();
        dto.setStatus("published");
        assertThat(dto.getStatus()).isEqualTo("published");
    }

    @Test
    void shouldSetAndGetDeployType() {
        DeploymentDTO dto = new DeploymentDTO();
        dto.setDeployType("HOT_LOAD");
        assertThat(dto.getDeployType()).isEqualTo("HOT_LOAD");
    }

    @Test
    void shouldHandleNullValues() {
        DeploymentDTO dto = new DeploymentDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getVersion()).isNull();
        assertThat(dto.getStatus()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DeploymentDTO dto = new DeploymentDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
