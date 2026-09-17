package com.zifang.z.lc.common.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 重试注解 — 蒸馏自 ace-platform-core {@code Retry} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标记在需要"失败自动重试"的方法上, 由 {@code ZLcRetryAspect} 切面拦截,
 * 在方法抛出异常时按 {@link #retryAttempts()} 次数重试,
 * 重试间隔 {@link #sleepInterval()} 毫秒, 可通过 {@link #ignoreExceptions()}
 * 指定"不需要重试"的异常类型.
 *
 * <p>典型用法:
 * <pre>{@code
 * @ZLcRetry(retryAttempts = 3, sleepInterval = 1000,
 *           ignoreExceptions = {BusinessException.class})
 * public Result<RemoteDTO> callRemote() {
 *     ...
 * }
 * }</pre>
 *
 * @author zifang
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ZLcRetry {

    /**
     * 重试次数 (不含首次调用) — 默认 3 次.
     */
    int retryAttempts() default 3;

    /**
     * 重试间隔 (毫秒) — 默认 5000ms.
     */
    long sleepInterval() default 5000L;

    /**
     * 不需要重试的异常类型 — 命中这些异常时直接抛出, 不再重试.
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};
}