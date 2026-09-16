package com.zifang.z.lc.web.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * z-lc 模块自动配置入口 (Spring Boot 自动注册).
 * <p>
 * 通过多个 {@link ComponentScan} 显式列出非 mapper 子包. mapper 包 {@code com.zifang.z.lc.mapper.*}
 * 在 z-lc-core 模块中独立存放 (避免与 {@code core.event} / {@code core.executor} 同级引起 @ComponentScan 与 @MapperScan
 * 同名 bean 冲突).
 * <p>
 * 数据源 / MapperScan 配置在 {@link LcModuleDataSource} (独立 sqlSessionFactoryLc).
 * <p>
 * HTTP 能力: 不再在本模块声明 RestTemplate bean — z-util-http 的
 * {@link com.zifang.util.http.client.HttpExecutor} 已自带 OkHttpClient 单例,
 * Adapter 直接 {@code HttpExecutor.getDefault()} 即可.
 */
@Configuration
@ComponentScans({
        @ComponentScan(basePackages = "com.zifang.z.lc.common"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.adapter"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.pipeline"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.pipeline.config"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.schema"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.materialize"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.event"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.executor"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.mapper"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.app"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.dict"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.viewconfig"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.relation"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.deployment"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.permission"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.ai"),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.workflow"),
        @ComponentScan(basePackages = "com.zifang.z.lc.design"),
        @ComponentScan(basePackages = "com.zifang.z.lc.sdk"),
        @ComponentScan(
                basePackages = "com.zifang.z.lc.web",
                excludeFilters = @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {com.zifang.z.lc.web.config.LcModuleDataSource.class}
                )
        )
})
public class LcAutoConfiguration {

    private static final Logger log = LogManager.getLogger(LcAutoConfiguration.class);

    public LcAutoConfiguration() {
        log.info("LcAutoConfiguration loaded — datasource/mapper in LcModuleDataSource, HTTP layer delegated to z-util-http HttpExecutor");
    }
}
