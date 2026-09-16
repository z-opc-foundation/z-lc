package com.zifang.z.lc.web.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
                "com.zifang.z.lc.core.materialize.mapper"
        },
        sqlSessionFactoryRef = "sqlSessionFactoryLc"
)
public class LcModuleDataSource extends ModuleDataSourceTemplate {

    @Bean("dataSourceLc")
    @ConditionalOnMissingBean(name = "dataSourceLc")
    public DataSource dataSource(Environment env) {
        return buildDataSource(env, "lc");
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
}
