package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcTaskExecutionUtil 单元测试
 *
 * @author zifang
 */
class ZLcTaskExecutionUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        java.lang.reflect.Constructor<ZLcTaskExecutionUtil> constructor = ZLcTaskExecutionUtil.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldExecuteTaskSuccessfully() {
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> "success";

        String result = ZLcTaskExecutionUtil.execute(task, 1, 0L);
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldRetryTaskOnFailure() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("Failed");
            }
            return "success";
        };

        String result = ZLcTaskExecutionUtil.execute(task, 3, 10L);
        assertThat(result).isEqualTo("success");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void shouldThrowExceptionAfterRetriesExhausted() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            throw new RuntimeException("Always fails");
        };

        assertThatThrownBy(() -> ZLcTaskExecutionUtil.execute(task, 3, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Always fails");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void shouldIgnoreSpecificExceptions() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            if (counter.get() < 2) {
                throw new RuntimeException("Failed", new IOException("IO Error"));
            }
            return "success";
        };

        String result = ZLcTaskExecutionUtil.execute(task, 3, 10L, IOException.class);
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldNotIgnoreNonSpecifiedExceptions() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            throw new IllegalArgumentException("Should fail");
        };

        assertThatThrownBy(() -> ZLcTaskExecutionUtil.execute(task, 3, 10L, IOException.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldHandleZeroRetryAttempts() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            return "success";
        };

        // retryAttempts < 1 should be normalized to 1
        String result = ZLcTaskExecutionUtil.execute(task, 0, 10L);
        assertThat(result).isEqualTo("success");
        assertThat(counter.get()).isEqualTo(1);
    }
}