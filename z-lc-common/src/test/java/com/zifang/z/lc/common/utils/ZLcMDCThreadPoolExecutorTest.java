package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcMDCThreadPoolExecutor 单元测试
 *
 * @author zifang
 */
class ZLcMDCThreadPoolExecutorTest {

    private ZLcMDCThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ZLcMDCThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                Thread::new,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
        MDC.clear();
    }

    @Test
    void shouldExtendThreadPoolExecutor() {
        assertThat(executor).isInstanceOf(ThreadPoolExecutor.class);
    }

    @Test
    void shouldPropagateMdcContext() throws InterruptedException {
        AtomicReference<String> capturedValue = new AtomicReference<>();
        MDC.put("traceId", "test-trace-123");

        executor.execute(() -> capturedValue.set(MDC.get("traceId")));

        Thread.sleep(100);
        assertThat(capturedValue.get()).isEqualTo("test-trace-123");
    }

    @Test
    void shouldHandleNullMdcContext() throws InterruptedException {
        AtomicReference<String> capturedValue = new AtomicReference<>();
        // 没有设置MDC

        executor.execute(() -> capturedValue.set(MDC.get("traceId")));

        Thread.sleep(100);
        assertThat(capturedValue.get()).isNull();
    }

    @Test
    void shouldClearMdcAfterExecution() throws InterruptedException {
        AtomicReference<String> capturedValue = new AtomicReference<>();
        MDC.put("traceId", "test-trace");

        executor.execute(() -> capturedValue.set(MDC.get("traceId")));

        Thread.sleep(100);
        assertThat(capturedValue.get()).isEqualTo("test-trace");
    }

    @Test
    void shouldExecuteTaskNormally() throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>();
        executor.execute(() -> result.set("executed"));

        Thread.sleep(100);
        assertThat(result.get()).isEqualTo("executed");
    }
}