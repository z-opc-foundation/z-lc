package com.zifang.z.lc.common.dto.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceTableColumnDTO 单元测试
 *
 * @author zifang
 */
class DataSourceTableColumnDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetDatasourceTableColumnId() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setDatasourceTableColumnId(123L);
        assertThat(dto.getDatasourceTableColumnId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetDatasourceCode() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setDatasourceCode("ds-001");
        assertThat(dto.getDatasourceCode()).isEqualTo("ds-001");
    }

    @Test
    void shouldSetAndGetTableName() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setTableName("users");
        assertThat(dto.getTableName()).isEqualTo("users");
    }

    @Test
    void shouldSetAndGetColumnName() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setColumnName("id");
        assertThat(dto.getColumnName()).isEqualTo("id");
    }

    @Test
    void shouldSetAndGetColumnType() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setColumnType("bigint");
        assertThat(dto.getColumnType()).isEqualTo("bigint");
    }

    @Test
    void shouldSetAndGetColumnLength() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setColumnLength("255");
        assertThat(dto.getColumnLength()).isEqualTo("255");
    }

    @Test
    void shouldSetAndGetColumnComment() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setColumnComment("用户ID");
        assertThat(dto.getColumnComment()).isEqualTo("用户ID");
    }

    @Test
    void shouldSetAndGetPrimaryKey() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setPrimaryKey(true);
        assertThat(dto.getPrimaryKey()).isTrue();
    }

    @Test
    void shouldHandleNullValues() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        assertThat(dto.getDatasourceTableColumnId()).isNull();
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getTableName()).isNull();
        assertThat(dto.getColumnName()).isNull();
        assertThat(dto.getColumnType()).isNull();
        assertThat(dto.getColumnLength()).isNull();
        assertThat(dto.getColumnComment()).isNull();
        assertThat(dto.getPrimaryKey()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldGenerateNativeSignature() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setTableName("users");
        dto.setColumnType("bigint");
        dto.setColumnComment("用户ID");
        
        String signature = dto.nativeSignature();
        assertThat(signature).isEqualTo("users:bigint:用户ID");
    }

    @Test
    void shouldGeneratePhysicalSignature() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setTableName("users");
        dto.setColumnType("varchar");
        dto.setColumnComment("用户名");
        
        String signature = dto.physicalSignature();
        assertThat(signature).isEqualTo("users:varchar:用户名");
    }

    @Test
    void shouldGenerateSignatureWithNullValues() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setTableName(null);
        dto.setColumnType(null);
        dto.setColumnComment(null);
        
        String signature = dto.nativeSignature();
        assertThat(signature).isEqualTo("null:null:null");
    }

    @Test
    void shouldSetEmptyStrings() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setDatasourceCode("");
        dto.setTableName("");
        dto.setColumnName("");
        dto.setColumnType("");
        dto.setColumnLength("");
        dto.setColumnComment("");
        
        assertThat(dto.getDatasourceCode()).isEmpty();
        assertThat(dto.getTableName()).isEmpty();
        assertThat(dto.getColumnName()).isEmpty();
        assertThat(dto.getColumnType()).isEmpty();
        assertThat(dto.getColumnLength()).isEmpty();
        assertThat(dto.getColumnComment()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DataSourceTableColumnDTO dto = new DataSourceTableColumnDTO();
        dto.setDatasourceCode(null);
        dto.setTableName(null);
        dto.setColumnName(null);
        dto.setColumnType(null);
        dto.setColumnLength(null);
        dto.setColumnComment(null);
        
        assertThat(dto.getDatasourceCode()).isNull();
        assertThat(dto.getTableName()).isNull();
        assertThat(dto.getColumnName()).isNull();
        assertThat(dto.getColumnType()).isNull();
        assertThat(dto.getColumnLength()).isNull();
        assertThat(dto.getColumnComment()).isNull();
    }
}