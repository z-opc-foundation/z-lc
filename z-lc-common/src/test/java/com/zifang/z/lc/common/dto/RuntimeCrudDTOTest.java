package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuntimeCrudDTO 单元测试
 *
 * @author zifang
 */
class RuntimeCrudDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetTenantCode() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setEntityCode("entity-001");
        assertThat(dto.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetFieldValues() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("field1", "value1");
        fieldValues.put("field2", 123);
        
        dto.setFieldValues(fieldValues);
        
        assertThat(dto.getFieldValues()).isEqualTo(fieldValues);
    }

    @Test
    void shouldSetAndGetParentEventId() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setParentEventId("parent-event-123");
        assertThat(dto.getParentEventId()).isEqualTo("parent-event-123");
    }

    @Test
    void shouldInitializeFieldValues() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        assertThat(dto.getFieldValues()).isNotNull();
        assertThat(dto.getFieldValues()).isEmpty();
    }

    @Test
    void shouldHandleNullValues() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getFieldValues()).isNotNull();
        assertThat(dto.getParentEventId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
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
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setTenantCode(null);
        dto.setAppCode(null);
        dto.setEntityCode(null);
        dto.setParentEventId(null);
        
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getParentEventId()).isNull();
    }

    @Test
    void shouldSetEmptyFieldValues() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setFieldValues(new HashMap<>());
        assertThat(dto.getFieldValues()).isEmpty();
    }

    @Test
    void shouldSetNullFieldValues() {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setFieldValues(null);
        assertThat(dto.getFieldValues()).isNull();
    }
}