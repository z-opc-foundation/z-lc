package com.zifang.z.lc.common.dto.datasource;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceTableDTO 单元测试
 *
 * @author zifang
 */
class DataSourceTableDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetDatasourceTableId() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceTableId(123L);
        assertThat(dto.getDatasourceTableId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetDatasourceCode() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceCode("ds-001");
        assertThat(dto.getDatasourceCode()).isEqualTo("ds-001");
    }

    @Test
    void shouldSetAndGetTableName() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setTableName("users");
        assertThat(dto.getTableName()).isEqualTo("users");
    }

    @Test
    void shouldSetAndGetDescriptions() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDescriptions("用户表");
        assertThat(dto.getDescriptions()).isEqualTo("用户表");
    }

    @Test
    void shouldSetAndGetColumns() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        DataSourceTableColumnDTO column1 = new DataSourceTableColumnDTO();
        column1.setColumnName("id");
        DataSourceTableColumnDTO column2 = new DataSourceTableColumnDTO();
        column2.setColumnName("name");
        
        List<DataSourceTableColumnDTO> columns = Arrays.asList(column1, column2);
        dto.setColumns(columns);
        
        assertThat(dto.getColumns()).hasSize(2);
        assertThat(dto.getColumns().get(0).getColumnName()).isEqualTo("id");
        assertThat(dto.getColumns().get(1).getColumnName()).isEqualTo("name");
    }

    @Test
    void shouldHandleNullValues() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        assertThat(dto.getDatasourceTableId()).isNull();
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getTableName()).isNull();
        assertThat(dto.getDescriptions()).isNull();
        assertThat(dto.getColumns()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldGenerateComponentCode() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceCode("ds-001");
        dto.setTableName("users");
        
        String componentCode = dto.getComponentCode();
        assertThat(componentCode).isEqualTo("ds-001:users");
    }

    @Test
    void shouldGenerateComponentCodeWithNullValues() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceCode(null);
        dto.setTableName(null);
        
        String componentCode = dto.getComponentCode();
        assertThat(componentCode).isEqualTo("null:null");
    }

    @Test
    void shouldSetEmptyStrings() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceCode("");
        dto.setTableName("");
        dto.setDescriptions("");
        
        assertThat(dto.getDatasourceCode()).isEmpty();
        assertThat(dto.getTableName()).isEmpty();
        assertThat(dto.getDescriptions()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setDatasourceCode(null);
        dto.setTableName(null);
        dto.setDescriptions(null);
        
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getTableName()).isNull();
        assertThat(dto.getDescriptions()).isNull();
    }

    @Test
    void shouldSetEmptyColumns() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setColumns(Arrays.asList());
        assertThat(dto.getColumns()).isEmpty();
    }

    @Test
    void shouldSetNullColumns() {
        DataSourceTableDTO dto = new DataSourceTableDTO();
        dto.setColumns(null);
        assertThat(dto.getColumns()).isNull();
    }
}