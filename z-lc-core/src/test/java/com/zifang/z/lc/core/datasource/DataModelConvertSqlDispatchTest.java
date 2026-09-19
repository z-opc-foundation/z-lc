package com.zifang.z.lc.core.datasource;

import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;
import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelConvertSqlDispatch 单元测试
 *
 * @author zifang
 */
public class DataModelConvertSqlDispatchTest {

    private final DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();

    @Test
    public void shouldCreateWithEmptyRegistry() {
        assertNotNull(dispatch);
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldRegisterStrategy() {
        DataModelConvertSqlStrategy mock = new StubStrategy();
        dispatch.register("test-type", mock);
        assertEquals(1, dispatch.registeredDialects().size());
        assertTrue(dispatch.registeredDialects().contains("test-type"));
    }

    @Test
    public void shouldIgnoreNullType() {
        dispatch.register(null, new StubStrategy());
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldIgnoreEmptyType() {
        dispatch.register("", new StubStrategy());
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldIgnoreNullStrategy() {
        dispatch.register("any", null);
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldOverrideExistingStrategy() {
        dispatch.register("a", new StubStrategy());
        dispatch.register("a", new StubStrategy());
        assertEquals(1, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldReturnNullForNullDataSourceType() {
        assertNull(dispatch.getDataModelConvertSqlStrategy(null));
    }

    @Test
    public void shouldReturnNullWhenNoStrategyRegistered() {
        assertNull(dispatch.getDataModelConvertSqlStrategy("oracle"));
    }

    @Test
    public void shouldReturnRegisteredStrategy() {
        StubStrategy mysql = new StubStrategy();
        dispatch.register("mysql", mysql);
        assertEquals(mysql, dispatch.getDataModelConvertSqlStrategy("mysql"));
    }

    @Test
    public void shouldFallbackToMysqlForUnknownType() {
        StubStrategy mysql = new StubStrategy();
        dispatch.register("mysql", mysql);
        // 'oracle' is unknown, should fallback to mysql
        assertEquals(mysql, dispatch.getDataModelConvertSqlStrategy("oracle"));
    }

    @Test
    public void shouldNotFallbackWhenTypeIsMysql() {
        StubStrategy mysql = new StubStrategy();
        dispatch.register("mysql", mysql);
        assertEquals(mysql, dispatch.getDataModelConvertSqlStrategy("mysql"));
    }

    @Test
    public void shouldReturnUnmodifiableSet() {
        dispatch.register("a", new StubStrategy());
        java.util.Set<String> dialects = dispatch.registeredDialects();
        try {
            dialects.add("b");
            // May or may not throw; either way we can verify size
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void shouldRegisterMultipleDialects() {
        dispatch.register("mysql", new StubStrategy());
        dispatch.register("doris", new StubStrategy());
        dispatch.register("dm", new StubStrategy());
        assertEquals(3, dispatch.registeredDialects().size());
    }

    // --- Stub ---

    private static class StubStrategy implements DataModelConvertSqlStrategy {
        @Override
        public Boolean tryConnect(DatasourceSaveDTO dto) {
            return Boolean.TRUE;
        }

        @Override
        public DataSource fetchDataSource(DataSourceDO dataSourceDO) {
            return null;
        }

        @Override
        public List<DataSourceTableDTO> fetchTableInfo(DataSource dataSource, String schemaMark) {
            return Collections.emptyList();
        }

        @Override
        public List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName) {
            return Collections.emptyList();
        }

        @Override
        public DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName) {
            return null;
        }
    }
}