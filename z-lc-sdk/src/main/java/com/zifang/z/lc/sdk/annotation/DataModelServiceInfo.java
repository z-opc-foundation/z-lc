package com.zifang.z.lc.sdk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 低代码模型服务的元信息.
 *
 * <p>蒸馏自 ace-platform-engine {@code DataModelServiceInfo}（{@code com.c2f.ace.engine.define}）。
 *
 * <p>业务方在 {@code AbstractDataModelService} 子类上标记本注解，
 * z-lc 引擎扫描时读取 QL 表达式，决定该服务适配哪些 (appCode, modelCode) 请求 —
 * 实现「同一模型多实现路由」（类似 ace 的 InnerDataModelServiceAdapter 决策机制）。
 *
 * <p>关键字段：
 * <ul>
 *   <li>{@link #appCode()}：绑定的应用编码（与 {@code @DataModel.appCode} 一致时可省略）</li>
 *   <li>{@link #modelCode()}：绑定的模型编码（与泛型 T 上的 {@code @DataModel.modelCode} 一致时可省略）</li>
 *   <li>{@link #expression()}：QL 表达式（如 {@code "tenantCode = 'a'"}），运行时由 QL 引擎评估，
 *       命中才把请求分发到本服务 — 用于灰度 / 多租户差异 / 业务覆盖</li>
 *   <li>{@link #exportRpc()}：是否把本服务以 Dubbo RPC 接口对外暴露（默认 false，
 *       即进程内 Spring Bean 调用；true 时引擎同时暴露 RPC 接口供远程调用）</li>
 * </ul>
 *
 * <p>用法：
 * <pre>{@code
 * @Service
 * @DataModelServiceInfo(appCode = "crm", modelCode = "customer",
 *                       expression = "tenantCode = 'a'")
 * public class CustomerServiceImpl extends AbstractDataModelService<Customer> { ... }
 * }</pre>
 *
 * @author zifang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataModelServiceInfo {

    /**
     * 绑定的应用编码.
     * <p>留空时回退到泛型 T 上的 {@code @DataModel.appCode}.
     */
    String appCode() default "";

    /**
     * 绑定的模型编码.
     * <p>留空时回退到泛型 T 上的 {@code @DataModel.modelCode}.
     */
    String modelCode() default "";

    /**
     * QL 表达式（如 {@code "tenantCode = 'a'" 或 "version > 0"}），
     * 引擎在路由时通过 QL 引擎求值；命中才使用本服务.
     */
    String expression() default "";

    /**
     * 是否同时以 Dubbo RPC 接口对外暴露本服务.
     * <p>z-lc 默认 false — 进程内 Spring Bean 调用；设为 true 时引擎同时
     * 把本服务注册为 RPC Provider，供远端 consumer 跨进程调用.
     */
    boolean exportRpc() default false;
}
