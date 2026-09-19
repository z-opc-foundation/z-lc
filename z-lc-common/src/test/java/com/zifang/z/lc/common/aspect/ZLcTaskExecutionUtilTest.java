package com.zifang.z.lc.common.aspect;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcTaskExecutionUtil (aspect包版本) 单元测试
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
    void shouldExecuteTaskSuccessfully() throws Exception {
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> "success";

        String result = ZLcTaskExecutionUtil.execute(task, 2, 10L);
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldRetryTaskOnFailure() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            if (counter.incrementAndGet() < 3) {
                throw new IOException("Failed");
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
            throw new IOException("Always fails");
        };

        assertThatThrownBy(() -> ZLcTaskExecutionUtil.execute(task, 2, 10L))
                .isInstanceOf(IOException.class)
                .hasMessage("Always fails");
    }

    @Test
    void shouldIgnoreSpecificExceptions() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            throw new IllegalArgumentException("Should fail immediately");
        };

        assertThatThrownBy(() -> ZLcTaskExecutionUtil.execute(task, 3, 10L, IllegalArgumentException.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldThrowForNullTask() {
        assertThatThrownBy(() -> ZLcTaskExecutionUtil.execute(null, 1, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task");
    }

    @Test
    void shouldHandleZeroRetryAttempts() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcTaskExecutionUtil.ZLcTask<String> task = () -> {
            counter.incrementAndGet();
            return "success";
        };

        String result = ZLcTaskExecutionUtil.execute(task, 0, 10L);
        assertThat(result).isEqualTo("success");
        assertThat(counter.get()).isEqualTo(1);
    }
}