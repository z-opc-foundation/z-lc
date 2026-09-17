package com.zifang.z.lc.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-lc 独立 Spring Boot 启动入口 (端到端验证用).
 * <p>
 * 设计哲学:
 * z-lc-bootstrap 是 z-lc 模块的"自带 demo" — 不依赖 main-starter / z-ctc / z-config,
 * 跑 z-lc-web 全部 Controller + z-lc-design 全部 SPI, 独立验证设计态框架可用.
 * <p>
 * 与 main-starter 的关系:
 * <ul>
 *   <li>main-starter 集成 z-lc + z-ctc + z-config + z-task + z-ops + z-agent + ...</li>
 *   <li>z-lc-bootstrap 只跑 z-lc, 用于 Phase 2A 集成验证</li>
 *   <li>生产部署走 main-starter; 本模块是开发期 demo</li>
 * </ul>
 * <p>
 * 启动后:
 * <ul>
 *   <li>端口 8888 (与 z-opc 现有 main 端口一致)</li>
 *   <li>/doc.html — Knife4j API 文档 (含 z-lc-design 分组)</li>
 *   <li>/api/lc/design/services — 列出所有已注册的低代码服务</li>
 *   <li>/api/lc/runtime/{entityCode}/{list|get|create|update|delete} — RuntimeCrud 入口</li>
 *   <li>/api/lc/health — 健康检查</li>
 * </ul>
 */
@SpringBootApplication
public class ZLcBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZLcBootstrapApplication.class, args);
    }
}
