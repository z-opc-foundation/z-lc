package com.zifang.z.lc.common.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 重试切面 — 蒸馏自 ace-platform-core
 * {@code RetryAspect} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>拦截标有 {@link ZLcRetry} 注解的方法, 在方法抛出异常时按注解配置重试.
 * 重试执行委托给 {@link ZLcTaskExecutionUtil}.
 *
 * <p>注意: 本切面依赖 spring-boot-starter-aop (AOP 运行时), z-lc-starter / z-lc-web
 * 模块引入 spring-boot-starter-aop 后即可自动注册 (通过 {@code @Aspect} 扫描).
 *
 * @author zifang
 */
@Aspect
@Component
public class ZLcRetryAspect {

    private static final Logger log = LogManager.getLogger(ZLcRetryAspect.class);

    @Around("@annotation(com.zifang.z.lc.common.aspect.ZLcRetry)")
    public Object pointcut(ProceedingJoinPoint pjp) throws Throwable {
        return retryableExecute(pjp);
    }

    protected Object retryableExecute(final ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        log.debug("[ZLcRetryAspect] 拦截方法: {}", method);

        ZLcRetry retry = method.getDeclaredAnnotation(ZLcRetry.class);
        if (retry == null) {
            return pjp.proceed();
        }

        int retryAttempts = retry.retryAttempts();
        long sleepInterval = retry.sleepInterval();
        Class<? extends Throwable>[] ignoreExceptions = retry.ignoreExceptions();

        ZLcTaskExecutionUtil.ZLcTask<Object> task = () -> {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                // 把 Throwable 包装成 Exception 透传给 ZLcTaskExecutionUtil
                if (e instanceof Exception) {
                    throw (Exception) e;
                }
                throw new RuntimeException(e);
            }
        };

        try {
            return ZLcTaskExecutionUtil.execute(task, retryAttempts, sleepInterval, ignoreExceptions);
        } catch (RuntimeException re) {
            // ZLcTaskExecutionUtil 在 ignoreExceptions 命中或 Error 场景下会包成 RuntimeException
            // 这里把 Throwable cause 还原后抛出
            Throwable cause = re.getCause();
            if (cause != null) {
                throw cause;
            }
            throw re;
        }
    }
}