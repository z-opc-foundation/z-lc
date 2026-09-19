package com.zifang.z.lc.common.dto.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceDO 单元测试
 *
 * @author zifang
 */
class DataSourceDOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        assertThat(dataSourceDO).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setId(123L);
        assertThat(dataSourceDO.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetTenantCode() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setTenantCode("tenant-001");
        assertThat(dataSourceDO.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetDatasourceCode() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setDatasourceCode("ds-001");
        assertThat(dataSourceDO.getDatasourceCode()).isEqualTo("ds-001");
    }

    @Test
    void shouldSetAndGetDatasourceType() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setDatasourceType("mysql");
        assertThat(dataSourceDO.getDatasourceType()).isEqualTo("mysql");
    }

    @Test
    void shouldSetAndGetJdbcUrl() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        assertThat(dataSourceDO.getJdbcUrl()).isEqualTo("jdbc:mysql://localhost:3306/test");
    }

    @Test
    void shouldSetAndGetUsername() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setUsername("root");
        assertThat(dataSourceDO.getUsername()).isEqualTo("root");
    }

    @Test
    void shouldSetAndGetPassword() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setPassword("password123");
        assertThat(dataSourceDO.getPassword()).isEqualTo("password123");
    }

    @Test
    void shouldSetAndGetSchemaMark() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setSchemaMark("public");
        assertThat(dataSourceDO.getSchemaMark()).isEqualTo("public");
    }

    @Test
    void shouldSetAndGetDescription() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setDescription("测试数据源");
        assertThat(dataSourceDO.getDescription()).isEqualTo("测试数据源");
    }

    @Test
    void shouldHandleNullValues() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        assertThat(dataSourceDO.getId()).isNull();
        assertThat(dataSourceDO.getTenantCode()).isNull();
        assertThat(dataSourceDO.getDatasourceCode()).isNull();
        assertThat(dataSourceDO.getDatasourceType()).isNull();
        assertThat(dataSourceDO.getJdbcUrl()).isNull();
        assertThat(dataSourceDO.getUsername()).isNull();
        assertThat(dataSourceDO.getPassword()).isNull();
        assertThat(dataSourceDO.getSchemaMark()).isNull();
        assertThat(dataSourceDO.getDescription()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        assertThat(dataSourceDO).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setTenantCode("");
        dataSourceDO.setDatasourceCode("");
        dataSourceDO.setDatasourceType("");
        dataSourceDO.setJdbcUrl("");
        dataSourceDO.setUsername("");
        dataSourceDO.setPassword("");
        dataSourceDO.setSchemaMark("");
        dataSourceDO.setDescription("");
        
        assertThat(dataSourceDO.getTenantCode()).isEmpty();
        assertThat(dataSourceDO.getDatasourceCode()).isEmpty();
        assertThat(dataSourceDO.getDatasourceType()).isEmpty();
        assertThat(dataSourceDO.getJdbcUrl()).isEmpty();
        assertThat(dataSourceDO.getUsername()).isEmpty();
        assertThat(dataSourceDO.getPassword()).isEmpty();
        assertThat(dataSourceDO.getSchemaMark()).isEmpty();
        assertThat(dataSourceDO.getDescription()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DataSourceDO dataSourceDO = new DataSourceDO();
        dataSourceDO.setTenantCode(null);
        dataSourceDO.setDatasourceCode(null);
        dataSourceDO.setDatasourceType(null);
        dataSourceDO.setJdbcUrl(null);
        dataSourceDO.setUsername(null);
        dataSourceDO.setPassword(null);
        dataSourceDO.setSchemaMark(null);
        dataSourceDO.setDescription(null);
        
        assertThat(dataSourceDO.getTenantCode()).isNull();
        assertThat(dataSourceDO.getDatasourceCode()).isNull();
        assertThat(dataSourceDO.getDatasourceType()).isNull();
        assertThat(dataSourceDO.getJdbcUrl()).isNull();
        assertThat(dataSourceDO.getUsername()).isNull();
        assertThat(dataSourceDO.getPassword()).isNull();
        assertThat(dataSourceDO.getSchemaMark()).isNull();
        assertThat(dataSourceDO.getDescription()).isNull();
    }
}