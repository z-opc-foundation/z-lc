package com.zifang.z.lc.common.utils;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;

/**
 * MDC 上下文传播线程池 — 蒸馏自 ace-platform-core
 * {@code MDCThreadPoolExecutor} ({@code com.c2f.ace.core.utils}).
 *
 * <p>继承 {@link ThreadPoolExecutor}, 在提交任务时自动将父线程的
 * SLF4J MDC 上下文传递给子线程, 保证日志链路追踪的一致性.
 *
 * <p>典型场景：
 * <ul>
 *   <li>异步任务中保持 traceId / spanId 等 MDC 字段</li>
 *   <li>线程池执行的业务逻辑需要统一日志上下文</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcMDCThreadPoolExecutor extends ThreadPoolExecutor {

    public ZLcMDCThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                     long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue,
                                     ThreadFactory threadFactory,
                                     RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(final Runnable runnable) {
        // 在提交时快照父线程的 MDC 上下文
        final Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(new Runnable() {
            @Override
            public void run() {
                // 将父线程的 MDC 内容传给子线程
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            }
        });
    }
}
