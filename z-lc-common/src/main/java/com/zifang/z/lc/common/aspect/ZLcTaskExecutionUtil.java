package com.zifang.z.lc.common.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 重试任务执行工具 — 蒸馏自 ace-platform-core {@code TaskExecutionUtil}.
 *
 * <p>通用"带退避重试 + 异常过滤"的任务执行器, 被 {@link ZLcRetryAspect} 调用.
 * 也可被业务方直接用于普通场景的重试 (例如 RPC 失败重试、数据库死锁重试等).
 *
 * <p>算法：
 * <ol>
 *   <li>执行 task, 若成功直接返回</li>
 *   <li>若抛异常且属于 {@code ignoreExceptions} 中任意一种, 直接抛出 (不再重试)</li>
 *   <li>否则等待 {@code sleepInterval} 毫秒, 然后重试, 最多 {@code retryAttempts} 次</li>
 *   <li>全部失败后抛出最后一次的异常</li>
 * </ol>
 *
 * @author zifang
 */
public final class ZLcTaskExecutionUtil {

    private static final Logger log = LogManager.getLogger(ZLcTaskExecutionUtil.class);

    private ZLcTaskExecutionUtil() {
        // utility class
    }

    /**
     * 函数式任务接口 — 类似 {@link Runnable} 但允许抛出异常 + 返回结果.
     */
    @FunctionalInterface
    public interface ZLcTask<T> {
        T execute() throws Exception;
    }

    /**
     * 执行 task 并按需重试.
     *
     * @param task            待执行任务
     * @param retryAttempts   重试次数 (不含首次调用)
     * @param sleepInterval   重试间隔 (毫秒)
     * @param ignoreExceptions 不需要重试的异常类型 — 命中时直接抛出
     * @param <T> 任务返回类型
     * @return 任务执行结果
     * @throws Exception 最终失败时的异常
     */
    @SafeVarargs
    public static <T> T execute(ZLcTask<T> task,
                                 int retryAttempts,
                                 long sleepInterval,
                                 Class<? extends Throwable>... ignoreExceptions) throws Exception {
        if (task == null) {
            throw new IllegalArgumentException("task 不能为空");
        }

        Throwable lastError = null;
        int totalAttempts = Math.max(retryAttempts, 0) + 1; // 首次 + 重试 N 次

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                return task.execute();
            } catch (Throwable e) {
                lastError = e;

                // 命中 ignoreExceptions 直接抛出, 不再重试
                if (shouldIgnore(e, ignoreExceptions)) {
                    log.debug("[ZLcRetry] 命中 ignoreExceptions, 不再重试: {}", e.getMessage());
                    throw asException(e);
                }

                if (attempt < totalAttempts) {
                    log.warn("[ZLcRetry] 第 {} 次执行失败, {}ms 后重试 (剩余 {} 次): {}",
                            attempt, sleepInterval, totalAttempts - attempt, e.getMessage());
                    try {
                        Thread.sleep(sleepInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("ZLcRetry 重试被中断", ie);
                    }
                } else {
                    log.error("[ZLcRetry] 已达最大重试次数 {}, 最后错误: {}",
                            retryAttempts, e.getMessage(), e);
                }
            }
        }

        // 所有重试用完, 抛出最后一次异常
        throw asException(lastError);
    }

    /**
     * 判断异常是否属于 "ignoreExceptions" 列表.
     */
    private static boolean shouldIgnore(Throwable e, Class<? extends Throwable>[] ignoreExceptions) {
        if (ignoreExceptions == null || ignoreExceptions.length == 0) {
            return false;
        }
        for (Class<? extends Throwable> ignoreType : ignoreExceptions) {
            if (ignoreType != null && ignoreType.isAssignableFrom(e.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Throwable 转成 Exception — {@link ZLcTask} 接口声明 throws Exception, 这里统一处理.
     */
    private static Exception asException(Throwable t) throws Exception {
        if (t instanceof Exception) {
            return (Exception) t;
        }
        // Error 也包装成 Exception 抛出
        return new Exception(t);
    }
}