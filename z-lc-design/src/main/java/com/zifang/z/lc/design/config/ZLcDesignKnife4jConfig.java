package com.zifang.z.lc.design.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;

/**
 * z-lc-design 的 OpenAPI 元信息 (最小化).
 * <p>
 * 设计哲学:
 * 与 z-lc 现有风格保持一致 (knife4j-openapi3 自动扫描 @Tag / @Operation, 自动渲染).
 * 本配置只输出"模块标题"日志 — 让 z-lc-design 在 doc.html 中作为独立分组 "z-lc 设计态" 展示,
 * 不强行覆盖 z-lc-web 已有 OpenAPI bean (避免冲突).
 * <p>
 * 注: 显式 OpenAPI bean 在多模块项目中容易冲突, 这里走"自动发现"路线更稳.
 */
@Configuration
public class ZLcDesignKnife4jConfig {

    private static final Logger log = LogManager.getLogger(ZLcDesignKnife4jConfig.class);

    public ZLcDesignKnife4jConfig() {
        log.info("z-lc-design OpenAPI 标签就绪: /api/lc/design/** (由 knife4j 自动扫描 @Tag)");
    }
}
