package com.zifang.z.lc.sdk.annotation;

import java.lang.annotation.*;

/**
 * 启用 z-lc 低代码模型服务扫描.
 * <p>
 * 设计哲学:
 * 业务模块在 @Configuration 类上标注 @EnableDataModelService, z-lc 引擎开启
 * AbstractDataModelService 子类扫描, 自动注册到 LowCodeModelServiceCollector.
 * <p>
 * 用法:
 * <pre>{@code
 * @Configuration
 * @EnableDataModelService(basePackages = "com.zifang.z.crm.model")
 * public class CrmConfig { }
 * }</pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableDataModelService {
    String[] basePackages() default {};
}
