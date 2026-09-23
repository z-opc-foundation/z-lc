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
 * 使用 z.base.db.lc.* 配置 (fallback 到 z.base.db.default.*).
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
        String jdbcUrl = firstString(binder, "z.base.db.lc.jdbc-url", "z.base.db.default.jdbc-url");
        if (jdbcUrl == null) {
            return buildDataSource(env, "lc");
        }
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(jdbcUrl);
        ds.setUsername(firstString(binder, "z.base.db.lc.username", "z.base.db.default.username"));
        ds.setPassword(firstString(binder, "z.base.db.lc.password", "z.base.db.default.password"));
        String driver = firstString(binder, "z.base.db.lc.driver-class-name", "z.base.db.default.driver-class-name");
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
