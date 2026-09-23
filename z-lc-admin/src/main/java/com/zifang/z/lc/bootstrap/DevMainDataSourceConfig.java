package com.zifang.z.lc.bootstrap;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * dev/standalone 的主数据源，刻意把连接池调成"会报错"而不是"会挂住"。
 * <p>
 * 背景：实测过一次现场 —— 跑完一轮真浏览器 E2E 之后，所有
 * {@code http-nio-*} 线程全部停在
 * {@code DruidDataSource.getConnectionInternal -> pollLast}，
 * 而**没有任何线程在执行查询**：连接被借走没还，池子空了。
 * 更糟的是 Druid 默认 {@code maxWait = -1}（无限等），于是表现成
 * "接口永久挂起、既不返回也不报错"，60s 客户端超时都等不到一个异常，
 * 排障时只能靠 jstack。生产上这就是整服务无响应。
 * <p>
 * 所以这里三件事：
 *  1. maxWait 给有限值 —— 池子空了要立刻抛，让人看见；
 *  2. removeAbandoned + logAbandoned —— 连接借走超时就被回收，并把**借走它的调用栈**打进日志，
 *     直接把"谁漏了连接"从玄学变成一条栈；
 *  3. keepAlive/校验沿用与 {@code ModuleDataSourceTemplate} 相同的做法，避免空闲连接被服务端掐掉。
 * <p>
 * 只作用于 standalone 的 dev profile；main-starter 那边有自己的主数据源，不在此范围。
 * 注意 removeAbandoned 官方不建议用于高负载生产，所以这里只在 dev/排障形态打开。
 */
@Configuration
@Profile("dev")
public class DevMainDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DevMainDataSourceConfig.class);

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setDriverClassName(properties.getDriverClassName());

        ds.setInitialSize(2);
        ds.setMinIdle(2);
        ds.setMaxActive(20);

        // 拿不到连接就在 10s 内抛 SQLException，绝不无限等（Druid 默认 -1 = 永远等）
        ds.setMaxWait(10_000L);

        // 泄漏的连接 60s 后强制回收，并打印借出时的调用栈
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(60);
        ds.setLogAbandoned(true);

        ds.setTestWhileIdle(true);
        ds.setValidationQuery("SELECT 1");
        ds.setKeepAlive(true);
        ds.setTimeBetweenEvictionRunsMillis(30_000L);
        ds.setMinEvictableIdleTimeMillis(60_000L);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setPoolPreparedStatements(true);
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);

        log.info("[z-lc dev] 主连接池已启用有限 maxWait=10s 与连接泄漏回收/栈打印 (maxActive=20)");
        return ds;
    }
}
