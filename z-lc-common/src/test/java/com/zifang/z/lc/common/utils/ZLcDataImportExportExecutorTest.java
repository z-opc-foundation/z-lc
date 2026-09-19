package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataImportExportExecutor 单元测试
 *
 * @author zifang
 */
class ZLcDataImportExportExecutorTest {

    private ExecutorService executorService;
    private ZLcDataImportExportExecutor executor;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        executor = new ZLcDataImportExportExecutor(executorService);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Test
    void shouldCreateExecutorWithExecutorService() {
        ZLcDataImportExportExecutor exec = new ZLcDataImportExportExecutor(executorService);
        assertThat(exec).isNotNull();
    }

    @Test
    void shouldNotExecuteWhenTaskCountIsNonZero() {
        AtomicInteger executed = new AtomicInteger(0);
        ZLcIDataImportOrExportProcess<String> process = new ZLcIDataImportOrExportProcess<String>() {
            @Override
            public void taskAbnormalInterrupt() {}

            @Override
            public void taskExecute(String task) {
                executed.incrementAndGet();
            }

            @Override
            public String nextTask() {
                return "next-task";
            }

            @Override
            public Long getInExecutionTaskCount() {
                return 1L; // 已有任务在执行
            }

            @Override
            public String getInExecutionTask() {
                return "current-task";
            }

            @Override
            public void updateTaskStatusToExecute(String task) {}
        };

        executor.taskQueuePop(process);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(executed.get()).isEqualTo(0);
    }

    @Test
    void shouldExecuteNextTaskWhenAvailable() {
        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger nextTaskCalled = new AtomicInteger(0);
        ZLcIDataImportOrExportProcess<String> process = new ZLcIDataImportOrExportProcess<String>() {
            @Override
            public void taskAbnormalInterrupt() {}

            @Override
            public void taskExecute(String task) {
                executed.incrementAndGet();
            }

            @Override
            public String nextTask() {
                nextTaskCalled.incrementAndGet();
                return "task-1";
            }

            @Override
            public Long getInExecutionTaskCount() {
                return 0L;
            }

            @Override
            public String getInExecutionTask() {
                return null;
            }

            @Override
            public void updateTaskStatusToExecute(String task) {}
        };

        executor.taskQueuePop(process);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(nextTaskCalled.get()).isEqualTo(1);
        assertThat(executed.get()).isEqualTo(1);
    }

    @Test
    void shouldNotExecuteWhenNextTaskIsNull() {
        AtomicInteger executed = new AtomicInteger(0);
        ZLcIDataImportOrExportProcess<String> process = new ZLcIDataImportOrExportProcess<String>() {
            @Override
            public void taskAbnormalInterrupt() {}

            @Override
            public void taskExecute(String task) {
                executed.incrementAndGet();
            }

            @Override
            public String nextTask() {
                return null;
            }

            @Override
            public Long getInExecutionTaskCount() {
                return 0L;
            }

            @Override
            public String getInExecutionTask() {
                return null;
            }

            @Override
            public void updateTaskStatusToExecute(String task) {}
        };

        executor.taskQueuePop(process);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(executed.get()).isEqualTo(0);
    }

    @Test
    void shouldAwaitTermination() throws InterruptedException {
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
    }
}