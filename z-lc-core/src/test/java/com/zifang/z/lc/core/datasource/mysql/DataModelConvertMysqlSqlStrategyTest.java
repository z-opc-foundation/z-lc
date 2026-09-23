package com.zifang.z.lc.core.datasource.mysql;

import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.core.datasource.DataModelConvertStrategyRegistrar;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void unreachableAddressFailsLoudInsteadOfReturningNull() {
        // 迁移前：反射建 Hikari 池，连不上也返回一个"看起来能用"的池（或 null）
        // 迁移后：z-util-jdbc 探活不过就不发布，直接抛 BusinessException
        DataSourceDO dsDO = new DataSourceDO();
        dsDO.setDatasourceCode("dead");
        dsDO.setJdbcUrl("jdbc:mysql://nonexistent-host-9999:3306/db");
        dsDO.setUsername("root");
        dsDO.setPassword("");
        try {
            assertNull(strategy.fetchDataSource(dsDO));
            fail("连不上应当抛出异常, 而不是返回 null 或不可用池");
        } catch (com.zifang.util.core.lang.exception.BusinessException expected) {
            assertTrue(expected.getMessage().contains("dead"));
        }
    }

    @Test
    public void mysqlUrlKeepsPlatformConnectionDefaults() {
        String plain = DataModelConvertMysqlSqlStrategy.withDefaults("jdbc:mysql://h:3306/db");
        assertTrue(plain.startsWith("jdbc:mysql://h:3306/db?"));
        assertTrue(plain.contains("characterEncoding=UTF-8"));
        assertTrue(plain.contains("serverTimezone=Asia/Shanghai"));
        // 已有参数串时用 & 追加, 不产生第二个 ?
        assertTrue(DataModelConvertMysqlSqlStrategy.withDefaults("jdbc:mysql://h:3306/db?allowMultiQueries=true")
                .contains("allowMultiQueries=true&useUnicode=true"));
    }

    @Test
    public void nonMysqlUrlIsPassedThroughUntouched() {
        // 整串 URL 交给方言识别, 不硬塞 MySQL 参数
        assertEquals("jdbc:postgresql://h:5432/db",
                DataModelConvertMysqlSqlStrategy.withDefaults("jdbc:postgresql://h:5432/db"));
        assertEquals("jdbc:h2:mem:lc;DB_CLOSE_DELAY=-1",
                DataModelConvertMysqlSqlStrategy.withDefaults("jdbc:h2:mem:lc;DB_CLOSE_DELAY=-1"));
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
}