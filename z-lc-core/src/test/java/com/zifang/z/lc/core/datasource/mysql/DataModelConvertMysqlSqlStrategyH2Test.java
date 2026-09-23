package com.zifang.z.lc.core.datasource.mysql;

import com.alibaba.druid.pool.DruidDataSource;
import com.zifang.util.core.lang.exception.BusinessException;
import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 数据源策略在真实连接上的行为：建池复用、换绑切池、探活不过不发布、关闭时释放。
 * <p>
 * 跑在 H2 内存库上，顺便证明建池/扫表扫列已不绑定 MySQL（迁移前反射拼 Hikari + 硬编码 mysql 驱动）。
 */
public class DataModelConvertMysqlSqlStrategyH2Test {

    private static final String URL = "jdbc:h2:mem:lc_strategy;DB_CLOSE_DELAY=-1";
    private static final String OTHER_URL = "jdbc:h2:mem:lc_strategy_b;DB_CLOSE_DELAY=-1";
    private static final String DEAD_URL = "jdbc:mysql://nonexistent-host-9999:3306/db";

    private DataModelConvertMysqlSqlStrategy strategy;

    @BeforeClass
    public static void seed() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE t_ticket (id BIGINT PRIMARY KEY, title VARCHAR(64), amount DECIMAL(12,2))");
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
    }

    @Before
    public void newStrategy() {
        strategy = new DataModelConvertMysqlSqlStrategy();
    }

    @After
    public void closeStrategy() {
        strategy.close();
    }

    private static DataSourceDO definition(String code, String jdbcUrl) {
        DataSourceDO ds = new DataSourceDO();
        ds.setDatasourceCode(code);
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setSchemaMark("PUBLIC");
        return ds;
    }

    @Test
    public void tryConnectAcceptsReachableAndRejectsUnreachable() {
        DatasourceSaveDTO alive = new DatasourceSaveDTO();
        alive.setJdbcUrl(URL);
        alive.setUsername("sa");
        alive.setPassword("");
        assertEquals(Boolean.TRUE, strategy.tryConnect(alive));

        DatasourceSaveDTO dead = new DatasourceSaveDTO();
        dead.setJdbcUrl(DEAD_URL);
        dead.setUsername("root");
        dead.setPassword("");
        assertEquals(Boolean.FALSE, strategy.tryConnect(dead));
    }

    @Test
    public void sameDefinitionReusesOnePool() {
        DataSource first = strategy.fetchDataSource(definition("lc", URL));
        DataSource second = strategy.fetchDataSource(definition("lc", URL));

        assertSame(first, second);
        assertTrue("连接池统一由 z-util-jdbc 建 Druid", first instanceof DruidDataSource);
    }

    @Test
    public void rebindingToAnotherAddressSwapsPoolAndClosesTheOldOne() {
        DataSource first = strategy.fetchDataSource(definition("lc", URL));
        DataSource second = strategy.fetchDataSource(definition("lc", OTHER_URL));

        assertNotSame(first, second);
        assertTrue("换绑后旧池必须关闭, 不能泄漏", ((DruidDataSource) first).isClosed());
    }

    @Test
    public void probeFailureKeepsThePublishedPool() {
        DataSource first = strategy.fetchDataSource(definition("lc", URL));
        try {
            strategy.fetchDataSource(definition("lc", DEAD_URL));
            fail("探活不过应当抛出, 而不是发布一个连不上的池");
        } catch (BusinessException expected) {
            assertTrue(expected.getMessage().contains("lc"));
        }
        assertSame(first, strategy.fetchDataSource(definition("lc", URL)));
    }

    @Test
    public void closeReleasesEveryPool() {
        DruidDataSource pool = (DruidDataSource) strategy.fetchDataSource(definition("lc", URL));
        strategy.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void metadataScanIsNotBoundToMysql() {
        DataSource ds = strategy.fetchDataSource(definition("lc", URL));

        List<String> names = new ArrayList<>();
        for (DataSourceTableDTO table : strategy.fetchTableInfo(ds, null)) {
            names.add(table.getTableName());
        }
        assertTrue(names.toString(), names.contains("T_TICKET"));

        DataSourceTableDTO ticket = strategy.fetchTableInfo(ds, null, "T_TICKET");
        assertEquals("T_TICKET", ticket.getTableName());
        List<DataSourceTableColumnDTO> columns = ticket.getColumns();
        assertEquals(3, columns.size());
        assertEquals("ID", columns.get(0).getColumnName());
        assertEquals("DECIMAL", columns.get(2).getColumnType());
        assertEquals("12,2", columns.get(2).getColumnLength());
    }
}
