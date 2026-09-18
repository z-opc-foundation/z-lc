package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppDTO 单元测试
 *
 * @author zifang
 */
class AppDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AppDTO dto = new AppDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetAppId() {
        AppDTO dto = new AppDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetTenantCode() {
        AppDTO dto = new AppDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        AppDTO dto = new AppDTO();
        dto.setAppCode("test_app");
        assertThat(dto.getAppCode()).isEqualTo("test_app");
    }

    @Test
    void shouldSetAndGetAppName() {
        AppDTO dto = new AppDTO();
        dto.setAppName("测试应用");
        assertThat(dto.getAppName()).isEqualTo("测试应用");
    }

    @Test
    void shouldSetAndGetStatus() {
        AppDTO dto = new AppDTO();
        dto.setStatus("active");
        assertThat(dto.getStatus()).isEqualTo("active");
    }

    @Test
    void shouldSetAndGetEntityCount() {
        AppDTO dto = new AppDTO();
        dto.setEntityCount(10);
        assertThat(dto.getEntityCount()).isEqualTo(10);
    }

    @Test
    void shouldHandleNullValues() {
        AppDTO dto = new AppDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getAppName()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getStatus()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AppDTO dto = new AppDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
