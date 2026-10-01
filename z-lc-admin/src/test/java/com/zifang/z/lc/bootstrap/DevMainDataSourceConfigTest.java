package com.zifang.z.lc.bootstrap;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁住 DevMainDataSourceConfig 的"防挂死"安全旋钮.
 * <p>
 * 这几个值一旦被改回去, 后果见 {@link DevMainDataSourceConfig} 的 javadoc:
 * 现场实测过 "Druid 默认 maxWait=-1 (无限等) -> 接口永久挂起、既不返回也不报错".
 * 测试目的: 有人改这些值时, 先看见本测试的红, 再去读 javadoc, 再决定改不改.
 */
class DevMainDataSourceConfigTest {

    private DataSource newDataSource() {
        DataSourceProperties props = new DataSourceProperties();
        props.setUrl("jdbc:h2:mem:dev-conf-test;DB_CLOSE_DELAY=-1");
        props.setUsername("sa");
        props.setPassword("");
        props.setDriverClassName("org.h2.Driver");
        return new DevMainDataSourceConfig().dataSource(props);
    }

    @Test
    @DisplayName("maxWait 必须是有限的 10s, 绝不能退回 Druid 默认的 -1 (无限等)")
    void maxWaitIsBoundedTenSeconds() {
        DruidDataSource ds = (DruidDataSource) newDataSource();
        assertEquals(10_000L, ds.getMaxWait(),
                "Druid 默认 maxWait=-1 (无限等), 实测会让接口永久挂起, 必须保持有限值");
    }

    @Test
    @DisplayName("连接泄漏检测必须开启, 否则借走不还的连接会无声消耗连接池")
    void removeAbandonedIsEnabled() {
        DruidDataSource ds = (DruidDataSource) newDataSource();
        assertTrue(ds.isRemoveAbandoned(), "removeAbandoned 必须开, 否则泄漏连接不会被回收");
        assertEquals(60, ds.getRemoveAbandonedTimeout(),
                "借出超时 60s 强制回收, 给出排查的窗口");
        assertTrue(ds.isLogAbandoned(),
                "logAbandoned 必须开, 把泄漏栈打进日志 (不开启的话 = 玄学排障)");
    }

    @Test
    @DisplayName("maxActive 必须留出真实并发余量, 而不是默认 8")
    void maxActiveIsReasonable() {
        DruidDataSource ds = (DruidDataSource) newDataSource();
        assertEquals(20, ds.getMaxActive(), "默认 8 在 E2E 高峰会先耗尽, 锁住 20");
        assertEquals(2, ds.getInitialSize());
        assertEquals(2, ds.getMinIdle());
    }

    @Test
    @DisplayName("返回的是 DruidDataSource (不是 HikariDataSource), 便于走 Druid 的泄漏日志")
    void beanIsDruidDataSource() {
        DataSource ds = newDataSource();
        assertNotNull(ds);
        assertEquals(DruidDataSource.class, ds.getClass());
    }
}
