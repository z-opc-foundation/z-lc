package com.zifang.z.lc.design.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-lc-design 模块自动配置入口.
 * <p>
 * 设计哲学:
 * z-lc 既有 LcAutoConfiguration (@ComponentScan basePackages="com.zifang.z.lc") 已经
 * 会扫描 z-lc-design 的所有 @Component. 本 AutoConfiguration 作为"模块标识" + 启动日志 —
 * 业务方启动应用时可在日志看到 "z-lc-design 模块已加载" 的明确信号.
 * <p>
 * 设计选择:
 * <ul>
 *   <li>不重复 @ComponentScan (避免覆盖既有扫描策略)</li>
 *   <li>提供 Knife4j Docket 分组 (z-lc-design 在 API 文档中独立展示)</li>
 *   <li>启动日志: 让业务方一眼看出"低代码设计态框架已就绪"</li>
 * </ul>
 */
@Configuration
@ComponentScan(basePackages = "com.zifang.z.lc.design")
public class ZLcDesignAutoConfiguration {

    private static final Logger log = LogManager.getLogger(ZLcDesignAutoConfiguration.class);

    public ZLcDesignAutoConfiguration() {
        log.info("z-lc-design 模块已加载 — 低代码设计态框架就绪 " +
                "(SPI: 23 个 / Collector: 启动时扫描 @LowCodeModelService)");
    }
}
