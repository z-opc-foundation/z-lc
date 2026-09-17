package com.zifang.z.lc.common.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * 任务执行工具 (带重试) — 蒸馏自 ace-platform-core
 * {@code TaskExecutionUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供带重试机制的任务执行能力, 支持指定重试次数、间隔和忽略异常类型.
 * 蒸馏时移除了 ace 对 BdpInvokerContext / UUID 的依赖, 改为纯 JDK 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>MQ 消息发送失败重试</li>
 *   <li>外部 HTTP 调用超时重试</li>
 *   <li>数据库写入冲突重试</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcTaskExecutionUtil {

    private ZLcTaskExecutionUtil() {
    }

    /**
     * 任务函数接口.
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    public interface ZLcTask<T> {
        T execute() throws RuntimeException;
    }

    /**
     * 执行任务, 支持重试和异常忽略.
     *
     * @param task             要执行的任务
     * @param retryAttempts    最大重试次数 (含首次调用)
     * @param sleepInterval    重试间隔 (毫秒)
     * @param ignoreExceptions 忽略的异常类型 (不抛出, 继续重试)
     * @param <T>              返回值类型
     * @return 任务执行结果
     * @throws RuntimeException 非忽略异常或重试次数耗尽
     */
    @SafeVarargs
    public static <T> T execute(ZLcTask<T> task, int retryAttempts, long sleepInterval,
                                Class<? extends Throwable>... ignoreExceptions) {
        if (retryAttempts < 1) {
            retryAttempts = 1;
        }

        Set<Class<? extends Throwable>> ignoreSet = new HashSet<>();
        if (ignoreExceptions != null) {
            for (Class<? extends Throwable> ex : ignoreExceptions) {
                ignoreSet.add(ex);
            }
        }

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                return task.execute();
            } catch (RuntimeException t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                // 检查是否为可忽略异常
                if (!ignoreSet.isEmpty()) {
                    boolean isIgnorable = false;
                    for (Class<? extends Throwable> ignoreClazz : ignoreSet) {
                        if (ignoreClazz.isAssignableFrom(cause.getClass())) {
                            isIgnorable = true;
                            break;
                        }
                    }
                    if (!isIgnorable) {
                        throw t;
                    }
                }
                // 最后一次重试仍失败, 抛出异常
                if (attempt >= retryAttempts) {
                    throw t;
                }
                // 等待后重试
                try {
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw t;
                }
            }
        }
        return null;
    }
}
