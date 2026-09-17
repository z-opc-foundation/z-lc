package com.zifang.z.lc.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 z-lc 模型服务的标记注解 — 蒸馏自 ace-platform-engine
 * {@code EnableModelService} （{@code com.c2f.ace.engine.define}}，
 * 行为完全对齐.
 *
 * <p>加在 Spring 配置类（如 {@code @Configuration}）上时，z-lc 引擎会扫描该类所在包
 * （或指定的 {@code basePackages}）下所有带 {@link DataModel @DataModel} 注解的 POJO，
 * 自动注册为低代码模型 — 业务方无需手动写 {@code @Bean} 注册.
 *
 * <p>典型用法：
 * <pre>{@code
 *   @Configuration
 *   @EnableModelService
 *   public class MyAppModelConfig {
 *   }
 * }</pre>
 *
 * <p>如果想自定义扫描包，可通过 {@link #basePackages()} 指定（默认扫描注解所在包）.
 *
 * @author zifang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableModelService {

    /**
     * 指定扫描包路径 — 默认空字符串表示扫描注解所在包及其子包.
     *
     * @return 扫描包路径数组（{@code String[] } 形式支持多包）
     */
    String[] basePackages() default {};
}
