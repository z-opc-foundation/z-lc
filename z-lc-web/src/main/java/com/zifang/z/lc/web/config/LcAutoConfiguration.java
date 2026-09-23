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
        // 整包扫描 core: 原来这里逐个枚举了 16 个 core.* 子包, 新增子包 (如 core.fieldtype)
        // 不会被扫到, 表现为"编译通过、bean 注入失败、启动直接挂", 而且报错完全不指向本文件.
        // mapper 接口由 LcModuleDataSource 的 @MapperScan 负责, 不在这里扫.
        @ComponentScan(
                basePackages = "com.zifang.z.lc.core",
                excludeFilters = @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.zifang\\.z\\.lc\\.mapper\\..*"
                )
        ),
        @ComponentScan(basePackages = "com.zifang.z.lc.core.mapper"),
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
