package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuntimeByIdDTO 单元测试
 *
 * @author zifang
 */
class RuntimeByIdDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetTenantCode() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setEntityCode("entity-001");
        assertThat(dto.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetId() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setId(123L);
        assertThat(dto.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetParentEventId() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setParentEventId("parent-event-123");
        assertThat(dto.getParentEventId()).isEqualTo("parent-event-123");
    }

    @Test
    void shouldHandleNullValues() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getParentEventId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setTenantCode("");
        dto.setAppCode("");
        dto.setEntityCode("");
        dto.setParentEventId("");
        
        assertThat(dto.getTenantCode()).isEmpty();
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getEntityCode()).isEmpty();
        assertThat(dto.getParentEventId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        RuntimeByIdDTO dto = new RuntimeByIdDTO();
        dto.setTenantCode(null);
        dto.setAppCode(null);
        dto.setEntityCode(null);
        dto.setParentEventId(null);
        
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getParentEventId()).isNull();
    }
}