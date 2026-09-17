package com.zifang.z.lc.common.annotation;

import java.lang.annotation.*;

/**
 * 自动重试注解 — 蒸馏自 ace-platform-core
 * {@code Retry} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标注在方法上, 配合 AOP 切面实现方法失败自动重试.
 * 适用于 RPC 调用、MQ 发送、外部 HTTP 请求等不稳定操作.
 *
 * <p>典型场景：
 * <ul>
 *   <li>流程启动后发送 MQ 消息失败重试</li>
 *   <li>审批通过后调用下游系统失败重试</li>
 *   <li>Apex 组织架构查询超时重试</li>
 * </ul>
 *
 * @author zifang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ZLcRetry {

    /** 最大重试次数 (含首次调用). */
    int retryAttempts() default 3;

    /** 重试间隔 (毫秒). */
    long sleepInterval() default 5000L;

    /** 忽略的异常类型 (不重试). */
    Class<? extends Throwable>[] ignoreExceptions() default {};
}
