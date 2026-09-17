package com.zifang.z.lc.common.utils;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 有序任务执行器 — 蒸馏自 ace-platform-core
 * {@code KeyOrderedExecutor} ({@code com.c2f.ace.core.utils}).
 *
 * <p>保证相同 key 的任务按提交顺序串行执行, 不同 key 之间可并行.
 * 适用于低代码平台的有序消息处理、有序数据同步等场景.
 *
 * <p>蒸馏时移除了 ace 对 Guava ThreadFactoryBuilder / MDCThreadPoolExecutor 的依赖,
 * 改为 JDK 原生 ThreadFactory 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>同一流程实例的消息按顺序处理</li>
 *   <li>同一用户的数据变更按顺序同步</li>
 *   <li>同一模型的事件按顺序消费</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcKeyOrderedExecutor {

    private final ExecutorService[] stripes;
    private final int stripeCount;

    /**
     * 构造有序执行器.
     *
     * @param stripeCount    条带数量 (越大并行度越高)
     * @param threadNamePrefix 线程名前缀
     */
    public ZLcKeyOrderedExecutor(int stripeCount, String threadNamePrefix) {
        if (stripeCount <= 0) {
            throw new IllegalArgumentException("stripeCount must be positive");
        }
        this.stripeCount = stripeCount;
        this.stripes = new ExecutorService[stripeCount];
        for (int i = 0; i < stripeCount; i++) {
            this.stripes[i] = new ThreadPoolExecutor(
                    1, 1, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(4096),
                    new ZLcKeyThreadFactory(threadNamePrefix + "-" + i),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
    }

    /**
     * 提交任务 (相同 key 串行执行).
     *
     * @param key  任务 key (用于路由到对应条带)
     * @param task 任务
     */
    public void execute(Object key, Runnable task) {
        Objects.requireNonNull(task, "task");
        stripes[indexFor(key)].execute(task);
    }

    /**
     * 提交任务并返回 Future.
     *
     * @param key  任务 key
     * @param task 任务
     * @param <V>  返回值类型
     * @return Future
     */
    public <V> Future<V> submit(Object key, Callable<V> task) {
        Objects.requireNonNull(task, "task");
        return stripes[indexFor(key)].submit(task);
    }

    /**
     * 优雅关闭.
     */
    public void shutdown() {
        for (ExecutorService executor : stripes) {
            executor.shutdown();
        }
    }

    /**
     * 立即关闭.
     */
    public void shutdownNow() {
        for (ExecutorService executor : stripes) {
            executor.shutdownNow();
        }
    }

    private int indexFor(Object key) {
        int h = (key == null) ? 0 : key.hashCode();
        h ^= (h >>> 16);
        int idx = h & 0x7fffffff;
        return idx % stripeCount;
    }

    /**
     * 自定义 ThreadFactory (JDK 原生).
     */
    private static class ZLcKeyThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        ZLcKeyThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        }
    }
}
