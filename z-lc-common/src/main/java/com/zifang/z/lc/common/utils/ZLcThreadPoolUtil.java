package com.zifang.z.lc.common.utils;

import java.util.concurrent.*;

/**
 * 线程池工具 — 蒸馏自 ace-platform-core
 * {@code ThreadPoolUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供预定义的通用线程池, 适用于低代码平台的异步任务处理:
 * 流程事件分发、MQ 消息发送、Excel 导入导出等场景.
 *
 * <p>蒸馏时移除了 ace 对 Guava ThreadFactoryBuilder 的依赖,
 * 改为 JDK 原生 ThreadFactory 实现.
 *
 * @author zifang
 */
public final class ZLcThreadPoolUtil {

    private ZLcThreadPoolUtil() {
    }

    /** 核心线程数. */
    private static final int CORE_POOL_SIZE = 30;

    /** 最大线程数. */
    private static final int MAX_POOL_SIZE = 300;

    /** 空闲线程存活时间 (毫秒). */
    private static final long KEEP_ALIVE_TIME = 3000L;

    /** 队列容量. */
    private static final int QUEUE_CAPACITY = 2000;

    /**
     * 通用异步线程池 — 适用于一般异步任务.
     * 拒绝策略: 调用者线程执行 (CallerRunsPolicy).
     */
    public static final ExecutorService SIMPLE_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ZLcThreadFactory("simple-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * IO 密集型线程池 — 适用于文件读写、HTTP 请求等 IO 操作.
     * 核心线程数较小, 最大线程数较大.
     */
    public static final ExecutorService IO_EXECUTOR = new ThreadPoolExecutor(
            10,
            200,
            5000L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(5000),
            new ZLcThreadFactory("io-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 定时任务线程池 — 适用于延迟/定时任务.
     */
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR =
            Executors.newScheduledThreadPool(10, new ZLcThreadFactory("schedule-pool"));

    /**
     * 自定义 ThreadFactory (JDK 原生, 无 Guava 依赖).
     */
    static class ZLcThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int counter = 0;

        ZLcThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + (++counter));
            t.setDaemon(false);
            t.setUncaughtExceptionHandler((thread, ex) ->
                    System.err.println("[ZLcThreadPool] Thread " + thread.getName() + " error: " + ex.getMessage()));
            return t;
        }
    }
}
