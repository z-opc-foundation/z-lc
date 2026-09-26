package com.zifang.z.lc.web.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 #52 的探活层：{@code /api/lc/health} 的 UP 必须是量出来的，不是写死的.
 * <p>
 * 这些用例逐条对着实测过的谎话写：部署件在 0 条可用连接、{@code /api/lc/app/list} 回
 * {@code http=500 Connection refused} 的同时回 200 {@code "status":"UP"}。
 * 把 status 改回硬编码 UP、把某个池从探活集合里摘掉、或探活时不还连接，本文件立刻红。
 */
class DataSourceHealthProberTest {

    private static final Object UNHANDLED = new Object();

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> iface, final Function<String, Object> answers) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        String name = method.getName();
                        if ("toString".equals(name)) {
                            return "fake:" + iface.getSimpleName();
                        }
                        if ("hashCode".equals(name)) {
                            return Integer.valueOf(System.identityHashCode(proxy));
                        }
                        if ("equals".equals(name)) {
                            return Boolean.valueOf(proxy == args[0]);
                        }
                        Object answered = answers.apply(name);
                        if (answered == UNHANDLED) {
                            throw new UnsupportedOperationException(iface.getSimpleName() + "." + name);
                        }
                        return answered;
                    }
                });
    }

    /** 一条"能用"的连接：跑 SELECT 1、报库名与 JDBC 串，并记录 close 有没有发生. */
    private static Connection workingConnection(final String url, final String product,
                                                final boolean selectReturnsRow,
                                                final AtomicBoolean connectionClosed,
                                                final AtomicBoolean statementClosed,
                                                final AtomicInteger queries) {
        ResultSet rs = fake(ResultSet.class, name -> {
            if ("next".equals(name)) {
                return Boolean.valueOf(selectReturnsRow);
            }
            if ("close".equals(name)) {
                return null;
            }
            return UNHANDLED;
        });
        Statement st = fake(Statement.class, name -> {
            if ("executeQuery".equals(name)) {
                queries.incrementAndGet();
                return rs;
            }
            if ("close".equals(name)) {
                statementClosed.set(true);
                return null;
            }
            return UNHANDLED;
        });
        DatabaseMetaData md = fake(DatabaseMetaData.class, name -> {
            if ("getDatabaseProductName".equals(name)) {
                return product;
            }
            if ("getDatabaseProductVersion".equals(name)) {
                return "8.0.26";
            }
            if ("getURL".equals(name)) {
                return url;
            }
            return UNHANDLED;
        });
        return fake(Connection.class, name -> {
            if ("createStatement".equals(name)) {
                return st;
            }
            if ("getMetaData".equals(name)) {
                return md;
            }
            if ("close".equals(name)) {
                connectionClosed.set(true);
                return null;
            }
            if ("isClosed".equals(name)) {
                return connectionClosed.get();
            }
            return UNHANDLED;
        });
    }

    private static DataSource pool(final Connection toGive, final String failureMessage, final long hangMs) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                if (hangMs > 0) {
                    try {
                        Thread.sleep(hangMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("probe was interrupted while waiting for a connection");
                    }
                }
                if (failureMessage != null) {
                    throw new SQLException(failureMessage);
                }
                if (toGive == null) {
                    throw new SQLException("no connection available");
                }
                return toGive;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return getConnection();
            }

            @Override
            public java.io.PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T unwrap(Class<T> iface) {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }
        };
    }

    private static DataSource healthyPool(final String url, final boolean selectReturnsRow,
                                          AtomicBoolean connectionClosed, AtomicBoolean statementClosed,
                                          AtomicInteger queries) {
        return pool(workingConnection(url, "MySQL", selectReturnsRow, connectionClosed, statementClosed, queries),
                null, 0L);
    }

    private static Map<String, DataSource> pools(Map<String, DataSource> into, String name, DataSource ds) {
        into.put(name, ds);
        return into;
    }

    @Test
    @DisplayName("缺陷#52：两个池都真跑得通才 UP，且逐个点名池、带库版本与耗时")
    void reportsUpOnlyFromRealProbes() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSource", healthyPool("jdbc:h2:mem:main", true,
                new AtomicBoolean(), new AtomicBoolean(), new AtomicInteger()));
        pools(map, "dataSourceLc", healthyPool("jdbc:mysql://127.0.0.1:33061/z_lc", true,
                new AtomicBoolean(), new AtomicBoolean(), new AtomicInteger()));

        Map<String, Object> report = new DataSourceHealthProber(1500L).report(map);

        assertEquals("UP", report.get("status"), "两个池都连通才该 UP: " + report);
        assertFalse(report.containsKey("reason"), "UP 时不该带 reason: " + report);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> sources =
                (java.util.List<Map<String, Object>>) report.get("sources");
        assertEquals(2, sources.size(), "每个池都要有一条明细: " + report);
        for (Map<String, Object> s : sources) {
            assertEquals("UP", s.get("status"), "明细: " + s);
            assertNotNull(s.get("name"), "明细要带池名: " + s);
            assertNotNull(s.get("database"), "明细要带库产品/版本: " + s);
            assertNotNull(s.get("latencyMs"), "明细要带实测耗时: " + s);
            assertFalse(String.valueOf(s.get("database")).contains("null"),
                    "库版本不该是拼出来的 null: " + s);
        }
        assertTrue(String.valueOf(sources.get(1).get("database")).contains("MySQL 8.0.26"),
                "database 要取到驱动报的真实版本: " + sources.get(1));
    }

    @Test
    @DisplayName("缺陷#52：一个池连不上就整体 DOWN，并点名是哪个池、带上真实失败原因")
    void reportsDownAndNamesTheFailingPool() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSource", healthyPool("jdbc:h2:mem:main", true,
                new AtomicBoolean(), new AtomicBoolean(), new AtomicInteger()));
        pools(map, "dataSourceLc", pool(null, "Connection refused", 0L));

        Map<String, Object> report = new DataSourceHealthProber(1500L).report(map);

        assertEquals("DOWN", report.get("status"),
                "LC 池连不上而整体还报 UP，就是当初那条谎: " + report);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> sources =
                (java.util.List<Map<String, Object>>) report.get("sources");
        Map<String, Object> lc = null;
        for (Map<String, Object> s : sources) {
            if ("dataSourceLc".equals(s.get("name"))) {
                lc = s;
            }
        }
        assertNotNull(lc, "明细里必须点得到坏的那个池: " + report);
        assertEquals("DOWN", lc.get("status"), "坏池自己要落 DOWN: " + lc);
        assertTrue(String.valueOf(lc.get("detail")).contains("Connection refused"),
                "detail 要带上驱动/池报的原文, 否则运维还得去 jstack: " + lc);
        // 好池不能被坏池连累成 DOWN —— 否则"哪个坏了"这条信息就丢了
        assertEquals(1, countUp(sources), "健康的那个池要照常报 UP: " + report);
        assertTrue(String.valueOf(report.get("reason")).contains("dataSourceLc"),
                "reason 要直接指认坏池: " + report);
    }

    private static int countUp(java.util.List<Map<String, Object>> sources) {
        int n = 0;
        for (Map<String, Object> s : sources) {
            if ("UP".equals(s.get("status"))) {
                n++;
            }
        }
        return n;
    }

    @Test
    @DisplayName("缺陷#52：池借干/挂住时健康检查自己不许被吊死 —— 探活必须有超时上限")
    void probeIsBoundedWhenPoolStarves() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSourceLc", pool(null, null, 6000L));

        long start = System.nanoTime();
        Map<String, Object> report = new DataSourceHealthProber(300L).report(map);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals("DOWN", report.get("status"), "拿不到连接就是 DOWN, 不能不回话: " + report);
        assertTrue(elapsedMs < 3000L,
                "探活必须在上限内返回 (实测 " + elapsedMs + "ms)，否则部署闸会被健康检查本身拖死");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> sources =
                (java.util.List<Map<String, Object>>) report.get("sources");
        assertTrue(String.valueOf(sources.get(0).get("detail")).contains("timed out"),
                "超时要写明是超时, 不是别的错: " + sources.get(0));
    }

    @Test
    @DisplayName("缺陷#52 连带缺陷#18：探活借走的连接必须还回去")
    void probeReleasesEveryConnectionItBorrowed() {
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean statementClosed = new AtomicBoolean();
        AtomicInteger queries = new AtomicInteger();
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSource", healthyPool("jdbc:h2:mem:main", true, closed, statementClosed, queries));

        new DataSourceHealthProber(1500L).report(map);

        assertEquals(1, queries.get(), "每个池至少真跑一次校验 SQL, 空转不算探活");
        assertTrue(closed.get(), "连接没还 = 每刷一次健康检查就漏一条连接, 池迟早被自己借干");
        assertTrue(statementClosed.get(), "Statement 也要关: " + statementClosed);
    }

    @Test
    @DisplayName("缺陷#52：校验 SQL 不返回行也要算 DOWN，不能把'跑完了'当'跑通了'")
    void emptyResultIsNotSuccess() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSource", healthyPool("jdbc:h2:mem:main", false,
                new AtomicBoolean(), new AtomicBoolean(), new AtomicInteger()));

        Map<String, Object> report = new DataSourceHealthProber(1500L).report(map);

        assertEquals("DOWN", report.get("status"), "SELECT 1 一行都没返回却报 UP = 又一处空转自证: " + report);
    }

    @Test
    @DisplayName("缺陷#52：一个池都没有时不许报 UP，也不许把'没有池'报成'全好'")
    void zeroPoolsIsNotHealthy() {
        Map<String, Object> report = new DataSourceHealthProber(1500L).report(new LinkedHashMap<String, DataSource>());

        assertEquals("DOWN", report.get("status"), "没有任何数据源却报 UP: " + report);
        assertTrue(String.valueOf(report.get("reason")).contains("DataSource"),
                "reason 要说清是没有池: " + report);
    }

    @Test
    @DisplayName("缺陷#52：JDBC 串可以回显给运维看，但凭证一个字都不许带出去")
    void neverLeaksCredentialsInHealthPayload() {
        AtomicBoolean closed = new AtomicBoolean();
        DataSource lc = healthyPool(
                "jdbc:mysql://root:S3cr3tInUserinfo@127.0.0.1:33061/z_lc?password=S3cr3tInParams&user=root",
                true, closed, new AtomicBoolean(), new AtomicInteger());
        Map<String, DataSource> map = new LinkedHashMap<>();
        pools(map, "dataSourceLc", lc);

        String rendered = String.valueOf(new DataSourceHealthProber(1500L).report(map));

        assertFalse(rendered.contains("S3cr3tInUserinfo"), "userinfo 段的密码漏出去了: " + rendered);
        assertFalse(rendered.contains("S3cr3tInParams"), "查询参数里的密码漏出去了: " + rendered);
        assertTrue(rendered.contains("jdbc:mysql://127.0.0.1:33061/z_lc"),
                "抹凭证不能顺手把 host/库名一起抹掉, 那等于没给排查信息: " + rendered);
    }
}
