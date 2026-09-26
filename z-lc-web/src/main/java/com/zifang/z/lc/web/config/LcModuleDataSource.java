package com.zifang.z.lc.web.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * z-lc 低代码模块数据源 + SqlSessionFactory 配置.
 * <p>
 * 连接串解析顺序 z.base.db.lc.jdbc-url -> z.base.db.default.jdbc-url -> spring.datasource.url;
 * 三个都不像 jdbc: 串时只有两种下场：显式给了 z.base.db.lc.host + database 才按模板拼，否则拒绝启动.
 * 独立 SqlSessionFactory (sqlSessionFactoryLc) 避免与其他模块的 6 个 factory 冲突.
 * <p>
 * E2E 修复: 加 @ConditionalOnMissingBean 让 main-starter 的 LcComponentScanConfig 优先
 * (因为 main-starter 的 LcComponentScanConfig.LcModuleDataSourceConfiguration 已经定义
 * 了 dataSourceLc + sqlSessionFactoryLc, 这里需要让位).
 */
@Configuration
@MapperScan(
        basePackages = {
                "com.zifang.z.lc.mapper.event",
                "com.zifang.z.lc.mapper.executor",
                "com.zifang.z.lc.mapper.dict",
                "com.zifang.z.lc.mapper.viewconfig",
                "com.zifang.z.lc.mapper.relation",
                "com.zifang.z.lc.mapper.deployment",
                "com.zifang.z.lc.mapper.permission",
                "com.zifang.z.lc.mapper.pipeline",
                "com.zifang.z.lc.mapper.workflow",
                "com.zifang.z.lc.mapper.undo",
                "com.zifang.z.lc.core.materialize.mapper"
        },
        sqlSessionFactoryRef = "sqlSessionFactoryLc"
)
public class LcModuleDataSource extends ModuleDataSourceTemplate {

    @Bean("dataSourceLc")
    @ConditionalOnMissingBean(name = "dataSourceLc")
    public DataSource dataSource(Environment env) {
        Binder binder = Binder.get(env);
        // 显式 jdbc-url 优先: z-boot 的 ModuleDataSourceTemplate 只读 host/port 并硬编码
        // jdbc:mysql://, 会让 z.base.db.*.jdbc-url / driver-class-name 静默失效 (H2 无法启用).
        // 第三档 spring.datasource.url 是给"单库部署"的：例如 local profile 的示例配置只写了
        // spring.datasource.*，此前会让 LC 池偷偷去连 localhost:3306。
        String jdbcUrl = firstString(binder,
                "z.base.db.lc.jdbc-url", "z.base.db.default.jdbc-url", "spring.datasource.url");
        if (!DataSourceConfigGuard.isUsable(jdbcUrl)) {
            String host = firstString(binder, "z.base.db.lc.host", "z.base.db.default.host");
            String database = firstString(binder, "z.base.db.lc.database", "z.base.db.default.database");
            if (host != null && database != null) {
                // 运维显式指了 host + 库名，模板拼出来的串是他要的串，不是我们猜的。
                return buildDataSource(env, "lc");
            }
            throw new IllegalStateException(
                    "z-lc 拒绝启动：模块数据源 dataSourceLc 没有可用的 JDBC 连接串。"
                            + " z.base.db.lc.jdbc-url / z.base.db.default.jdbc-url / spring.datasource.url"
                            + " 三个里没有任何一个配成 jdbc: 开头的值，当前解析到的值 = "
                            + DataSourceConfigGuard.describe(jdbcUrl) + "。"
                            + " 拒绝退回 ModuleDataSourceTemplate 的默认拼串 jdbc:mysql://localhost:3306/"
                            + "（database 为空、用户名 root、密码空）：实测那样进程会带着 0 条可用连接起来、"
                            + " /api/lc/health 照样回 UP，第一条业务查询才 http=500 Connection refused。"
                            + " 请给 --z.base.db.lc.jdbc-url=jdbc:mysql://<host>:<port>/z_lc?...，"
                            + " 或同时给 z.base.db.lc.host 与 z.base.db.lc.database 显式走模板。");
        }
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(jdbcUrl);
        ds.setUsername(firstString(binder, "z.base.db.lc.username", "z.base.db.default.username",
                "spring.datasource.username"));
        ds.setPassword(firstString(binder, "z.base.db.lc.password", "z.base.db.default.password",
                "spring.datasource.password"));
        String driver = firstString(binder, "z.base.db.lc.driver-class-name",
                "z.base.db.default.driver-class-name", "spring.datasource.driver-class-name");
        // 驱动与 url 的配套性也要判，且必须在这个池被 init 之前（下面没有任何一处调用 getConnection，
        // 第一次真连接要等 MyBatis 或健康检查）—— 见 DataSourceConfigGuard#driverUrlMismatch 的记录：
        // 驱动不受理这个 url 时 connect() 返回 null 而不是抛异常，池一旦起来建连线程就无退避死循环
        // （实测 8 分钟 13 GB 日志）。走 fallback 链时最容易凑出这一对：dev 的
        // spring.datasource.driver-class-name 是 org.h2.Driver，运维只把 z.base.db.lc.jdbc-url
        // 换成 jdbc:mysql:// 就配出了一个永远连不上的组合。
        String mismatch = DataSourceConfigGuard.driverUrlMismatch(driver, jdbcUrl);
        if (mismatch != null) {
            throw new IllegalStateException(
                    "z-lc 拒绝启动：模块数据源 dataSourceLc 的驱动与 JDBC 串对不上 —— " + mismatch
                            + "。配置项在 z.base.db.lc.driver-class-name / z.base.db.lc.jdbc-url"
                            + "（以及 default、spring.datasource 那两级 fallback）里。"
                            + " 这一支必须拦在建池之前：驱动对不受理的 url 是返回 null 而不是抛异常，"
                            + " 实测那样 Druid 建连线程会在 rawConn is null 上无退避死循环（8 分钟 13 GB 日志）。");
        }
        if (driver != null) {
            ds.setDriverClassName(driver);
        }
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(20);
        ds.setMaxWait(60000L);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery("SELECT 1");
        return ds;
    }

    @Bean("sqlSessionFactoryLc")
    @ConditionalOnMissingBean(name = "sqlSessionFactoryLc")
    public SqlSessionFactory sqlSessionFactoryLc(DataSource dataSourceLc) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSourceLc);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));
        factoryBean.setTypeAliasesPackage("com.zifang.z.lc.core.event.entity");
        return factoryBean.getObject();
    }

    private static String firstString(Binder binder, String... keys) {
        for (String key : keys) {
            String v = binder.bind(key, String.class).orElse(null);
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
