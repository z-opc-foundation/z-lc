package com.zifang.z.lc.web.it;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-lc-web 集成测试专用启动类.
 * <p>
 * 刻意放在叶子包 {@code com.zifang.z.lc.web.it}: {@code @SpringBootApplication} 默认只扫描本包及其子包,
 * 真正的组件装配交给 {@code spring.factories} 注册的 {@code LcAutoConfiguration} + {@code LcModuleDataSource}
 * (与生产 main-starter 完全一致的同一条自动配置路径), 从而在 H2 上跑通真实 MetaController 契约.
 */
@SpringBootApplication
public class LcTestApplication {
}
