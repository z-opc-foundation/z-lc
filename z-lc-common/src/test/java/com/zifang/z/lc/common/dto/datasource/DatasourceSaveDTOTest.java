package com.zifang.z.lc.common.dto.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatasourceSaveDTO 单元测试
 *
 * @author zifang
 */
class DatasourceSaveDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetDatasourceCode() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setDatasourceCode("ds-001");
        assertThat(dto.getDatasourceCode()).isEqualTo("ds-001");
    }

    @Test
    void shouldSetAndGetDatasourceType() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setDatasourceType("mysql");
        assertThat(dto.getDatasourceType()).isEqualTo("mysql");
    }

    @Test
    void shouldSetAndGetJdbcUrl() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        assertThat(dto.getJdbcUrl()).isEqualTo("jdbc:mysql://localhost:3306/test");
    }

    @Test
    void shouldSetAndGetUsername() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setUsername("root");
        assertThat(dto.getUsername()).isEqualTo("root");
    }

    @Test
    void shouldSetAndGetPassword() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setPassword("password123");
        assertThat(dto.getPassword()).isEqualTo("password123");
    }

    @Test
    void shouldSetAndGetSchemaMark() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setSchemaMark("public");
        assertThat(dto.getSchemaMark()).isEqualTo("public");
    }

    @Test
    void shouldSetAndGetDescription() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setDescription("测试数据源");
        assertThat(dto.getDescription()).isEqualTo("测试数据源");
    }

    @Test
    void shouldHandleNullValues() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getDatasourceType()).isNull();
        assertThat(dto.getJdbcUrl()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getPassword()).isNull();
        assertThat(dto.getSchemaMark()).isNull();
        assertThat(dto.getDescription()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setDatasourceCode("");
        dto.setDatasourceType("");
        dto.setJdbcUrl("");
        dto.setUsername("");
        dto.setPassword("");
        dto.setSchemaMark("");
        dto.setDescription("");
        
        assertThat(dto.getDatasourceCode()).isEmpty();
        assertThat(dto.getDatasourceType()).isEmpty();
        assertThat(dto.getJdbcUrl()).isEmpty();
        assertThat(dto.getUsername()).isEmpty();
        assertThat(dto.getPassword()).isEmpty();
        assertThat(dto.getSchemaMark()).isEmpty();
        assertThat(dto.getDescription()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        dto.setDatasourceCode(null);
        dto.setDatasourceType(null);
        dto.setJdbcUrl(null);
        dto.setUsername(null);
        dto.setPassword(null);
        dto.setSchemaMark(null);
        dto.setDescription(null);
        
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getDatasourceType()).isNull();
        assertThat(dto.getJdbcUrl()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getPassword()).isNull();
        assertThat(dto.getSchemaMark()).isNull();
        assertThat(dto.getDescription()).isNull();
    }
}