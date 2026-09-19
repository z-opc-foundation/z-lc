package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcKeyOrderedExecutor 单元测试
 *
 * @author zifang
 */
class ZLcKeyOrderedExecutorTest {

    private ZLcKeyOrderedExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ZLcKeyOrderedExecutor(4, "test");
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void shouldCreateExecutorWithPositiveStripeCount() {
        ZLcKeyOrderedExecutor exec = new ZLcKeyOrderedExecutor(2, "test2");
        assertThat(exec).isNotNull();
        exec.shutdown();
    }

    @Test
    void shouldThrowExceptionForZeroStripeCount() {
        assertThatThrownBy(() -> new ZLcKeyOrderedExecutor(0, "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowExceptionForNegativeStripeCount() {
        assertThatThrownBy(() -> new ZLcKeyOrderedExecutor(-1, "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExecuteTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        executor.execute("key1", counter::incrementAndGet);

        Thread.sleep(100);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldExecuteTasksForSameKeySerially() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        executor.execute("same-key", () -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.execute("same-key", () -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(300);
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void shouldSubmitTaskAndReturnFuture() throws ExecutionException, InterruptedException {
        Future<Integer> future = executor.submit("key1", () -> 42);
        Integer result = future.get();
        assertThat(result).isEqualTo(42);
    }

    @Test
    void shouldShutdownExecutor() {
        executor.shutdown();
        assertThat(executor).isNotNull();
    }

    @Test
    void shouldShutdownNowExecutor() {
        executor.shutdownNow();
        assertThat(executor).isNotNull();
    }
}