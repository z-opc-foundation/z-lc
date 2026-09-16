package com.zifang.z.lc.design.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 标记一个 Spring Bean 为"低代码服务" — z-lc 引擎自动扫描并注册到 LowCodeModelServiceCollector.
 * <p>
 * 设计哲学:
 * 用户在 z-lc 应用中实现一个 SPI (如 FormDataInitService), 同时在本 Bean 上加 @LowCodeModelService.
 * z-lc 启动时通过 ApplicationContext 扫所有标注本注解的 Bean, 按 (group, code) 索引到 Map.
 * <p>
 * 用法:
 * <pre>{@code
 *   @Component
 *   @LowCodeModelService
 *   public class MyFormInitImpl implements FormDataInitService {
 *       // ...
 *   }
 * }</pre>
 * <p>
 * 关键区别: 与 z-lc-sdk 的 @InterfaceMapping (元信息注解, 用于 SPI 自身) 不同 —
 * 本注解是"运行时注册标记", 用于 Collector 扫描.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Service
public @interface LowCodeModelService {
}
