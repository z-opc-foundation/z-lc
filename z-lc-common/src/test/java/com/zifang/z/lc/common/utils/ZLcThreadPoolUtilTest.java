package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcThreadPoolUtil 单元测试
 *
 * @author zifang
 */
class ZLcThreadPoolUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcThreadPoolUtil> constructor = ZLcThreadPoolUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveSimpleExecutor() {
        assertThat(ZLcThreadPoolUtil.SIMPLE_EXECUTOR).isNotNull();
        assertThat(ZLcThreadPoolUtil.SIMPLE_EXECUTOR).isInstanceOf(ExecutorService.class);
    }

    @Test
    void shouldHaveIoExecutor() {
        assertThat(ZLcThreadPoolUtil.IO_EXECUTOR).isNotNull();
        assertThat(ZLcThreadPoolUtil.IO_EXECUTOR).isInstanceOf(ExecutorService.class);
    }

    @Test
    void shouldHaveScheduledExecutor() {
        assertThat(ZLcThreadPoolUtil.SCHEDULED_EXECUTOR).isNotNull();
        assertThat(ZLcThreadPoolUtil.SCHEDULED_EXECUTOR).isInstanceOf(ScheduledExecutorService.class);
    }

    @Test
    void shouldExecuteTaskInSimpleExecutor() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcThreadPoolUtil.SIMPLE_EXECUTOR.submit(counter::incrementAndGet);

        Thread.sleep(100);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldNotShutdownExecutors() {
        // 注意: 不要关闭这些共享线程池, 它们被设计为全局共享
        assertThat(ZLcThreadPoolUtil.SIMPLE_EXECUTOR.isShutdown()).isFalse();
    }
}