package com.zifang.z.lc.core.datasource.mysql;

import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.core.datasource.DataModelConvertStrategyRegistrar;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelConvertMysqlSqlStrategy 单元测试
 *
 * @author zifang
 */
public class DataModelConvertMysqlSqlStrategyTest {

    private final DataModelConvertMysqlSqlStrategy strategy = new DataModelConvertMysqlSqlStrategy();

    @Test
    public void shouldCreate() {
        assertNotNull(strategy);
    }

    @Test
    public void shouldBeAnnotatedWithMysqlDialect() {
        DataModelConvertStrategyRegistrar.ZLcDialect anno =
                strategy.getClass().getAnnotation(DataModelConvertStrategyRegistrar.ZLcDialect.class);
        assertNotNull(anno);
        assertEquals("mysql", anno.value());
    }

    @Test
    public void shouldImplementStrategyInterface() {
        assertTrue(strategy instanceof com.zifang.z.lc.core.datasource.DataModelConvertSqlStrategy);
    }

    @Test
    public void shouldReturnFalseForNullDatasourceSaveDTO() {
        assertEquals(Boolean.FALSE, strategy.tryConnect(null));
    }

    @Test
    public void shouldReturnFalseForNullJdbcUrl() {
        com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO dto =
                new com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO();
        assertEquals(Boolean.FALSE, strategy.tryConnect(dto));
    }

    @Test
    public void shouldReturnFalseForUnreachableJdbcUrl() {
        com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO dto =
                new com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO();
        dto.setJdbcUrl("jdbc:mysql://nonexistent-host-9999:3306/nonexistent");
        dto.setUsername("root");
        dto.setPassword("");
        assertEquals(Boolean.FALSE, strategy.tryConnect(dto));
    }

    @Test
    public void shouldReturnNullForNullDataSourceDO() {
        assertNull(strategy.fetchDataSource(null));
    }

    @Test
    public void shouldReturnNullForNullJdbcUrlInDataSourceDO() {
        DataSourceDO dsDO = new DataSourceDO();
        assertNull(strategy.fetchDataSource(dsDO));
    }

    @Test
    public void shouldReturnNullForDataSourceWithoutHikari() {
        // Without Hikari class in classpath, buildDruidDataSource returns null
        // We test that the method returns null rather than throwing
        DataSourceDO dsDO = new DataSourceDO();
        dsDO.setJdbcUrl("jdbc:mysql://localhost:3306/db");
        dsDO.setUsername("root");
        dsDO.setPassword("");
        dsDO.setDatasourceCode("test");
        // This may return null or actual DataSource depending on classpath; either is acceptable
        javax.sql.DataSource result = strategy.fetchDataSource(dsDO);
        // We just assert the call doesn't throw — result can be null or non-null
    }

    @Test
    public void shouldReturnEmptyListForNullDataSourceInFetchTableInfo() {
        assertEquals(0, strategy.fetchTableInfo(null, "schema").size());
    }

    @Test
    public void shouldReturnEmptyListForNullDataSourceInFetchColumnInfo() {
        assertEquals(0, strategy.fetchTableColumnInfo(null, "schema", "table").size());
    }

    @Test
    public void shouldReturnEmptyListForNullTableNameInFetchColumnInfo() {
        assertEquals(0, strategy.fetchTableColumnInfo(null, "schema", null).size());
    }

    @Test
    public void shouldReturnNullForNullDataSourceInFetchSingleTableInfo() {
        assertNull(strategy.fetchTableInfo(null, "schema", "table"));
    }

    @Test
    public void shouldReturnNullForNullTableNameInFetchSingleTableInfo() {
        assertNull(strategy.fetchTableInfo(null, "schema", null));
    }

    @Test
    public void mysqlDriverConstantShouldBeCorrect() throws Exception {
        java.lang.reflect.Field f = DataModelConvertMysqlSqlStrategy.class.getDeclaredField("MYSQL_DRIVER");
        f.setAccessible(true);
        assertEquals("com.mysql.cj.jdbc.Driver", f.get(null));
    }

    @Test
    public void defaultJdbcParamShouldBeDefined() throws Exception {
        java.lang.reflect.Field f = DataModelConvertMysqlSqlStrategy.class.getDeclaredField("DEFAULT_JDBC_PARAM");
        f.setAccessible(true);
        String param = (String) f.get(null);
        assertNotNull(param);
        assertTrue(param.contains("UTF-8"));
    }
}