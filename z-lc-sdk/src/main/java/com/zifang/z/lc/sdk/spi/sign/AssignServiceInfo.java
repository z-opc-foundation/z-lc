package com.zifang.z.lc.sdk.spi.sign;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 签名任务服务 SPI 元信息 — 蒸馏自 ace-platform-engine {@code AssignServiceInfo}
 * （{@code com.c2f.ace.engine.define}），字段语义完全对齐.
 *
 * <p>业务方在 {@link AbstractAssignService} 子类上标记本注解 —
 * z-lc 引擎扫描时通过 {@code identityCode} 建立索引，通过 {@code exportRpc}
 * 决定是否对外暴露 RPC 接口.
 *
 * @author zifang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AssignServiceInfo {

    /**
     * 身份标识（业务唯一 — 对应具体 CA 厂商或签名类型）.
     */
    String identityCode();

    /**
     * QL 表达式（用于业务路由，命中才把请求分发到本服务）.
     */
    String expression() default "";

    /**
     * 是否同时以 Dubbo RPC 接口对外暴露本服务.
     */
    boolean exportRpc() default false;
}
