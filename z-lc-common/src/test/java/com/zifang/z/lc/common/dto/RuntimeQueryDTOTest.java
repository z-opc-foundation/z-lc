package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuntimeQueryDTO 单元测试
 *
 * @author zifang
 */
class RuntimeQueryDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetTenantCode() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setEntityCode("entity-001");
        assertThat(dto.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetFilters() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("userName", "张三");
        filters.put("age:gt", 18);
        
        dto.setFilters(filters);
        
        assertThat(dto.getFilters()).isEqualTo(filters);
    }

    @Test
    void shouldSetAndGetOrderBy() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setOrderBy("create_time desc");
        assertThat(dto.getOrderBy()).isEqualTo("create_time desc");
    }

    @Test
    void shouldSetAndGetPage() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setPage(5);
        assertThat(dto.getPage()).isEqualTo(5);
    }

    @Test
    void shouldSetAndGetSize() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setSize(50);
        assertThat(dto.getSize()).isEqualTo(50);
    }

    @Test
    void shouldInitializeFilters() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        assertThat(dto.getFilters()).isNotNull();
        assertThat(dto.getFilters()).isEmpty();
    }

    @Test
    void shouldInitializePageAndSize() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        assertThat(dto.getPage()).isEqualTo(1);
        assertThat(dto.getSize()).isEqualTo(20);
    }

    @Test
    void shouldHandleNullValues() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getFilters()).isNotNull();
        assertThat(dto.getOrderBy()).isNull();
        assertThat(dto.getPage()).isEqualTo(1);
        assertThat(dto.getSize()).isEqualTo(20);
    }

    @Test
    void shouldImplementSerializable() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setTenantCode("");
        dto.setAppCode("");
        dto.setEntityCode("");
        dto.setOrderBy("");
        
        assertThat(dto.getTenantCode()).isEmpty();
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getEntityCode()).isEmpty();
        assertThat(dto.getOrderBy()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setTenantCode(null);
        dto.setAppCode(null);
        dto.setEntityCode(null);
        dto.setOrderBy(null);
        
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getOrderBy()).isNull();
    }

    @Test
    void shouldSetEmptyFilters() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setFilters(new HashMap<>());
        assertThat(dto.getFilters()).isEmpty();
    }

    @Test
    void shouldSetNullFilters() {
        RuntimeQueryDTO dto = new RuntimeQueryDTO();
        dto.setFilters(null);
        assertThat(dto.getFilters()).isNull();
    }
}