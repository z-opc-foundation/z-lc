package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcIDataImportOrExportProcess 单元测试
 *
 * @author zifang
 */
class ZLcIDataImportOrExportProcessTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcIDataImportOrExportProcess.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveGenericTypeParameter() {
        assertThat(ZLcIDataImportOrExportProcess.class.getTypeParameters()).hasSize(1);
    }

    @Test
    void shouldImplementAllMethods() {
        ZLcIDataImportOrExportProcess<String> process = new ZLcIDataImportOrExportProcess<String>() {
            @Override
            public void taskAbnormalInterrupt() {
            }

            @Override
            public void taskExecute(String task) {
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
            public void updateTaskStatusToExecute(String task) {
            }
        };

        // 测试所有方法都能调用
        process.taskAbnormalInterrupt();
        process.taskExecute("test");
        assertThat(process.nextTask()).isNull();
        assertThat(process.getInExecutionTaskCount()).isEqualTo(0L);
        assertThat(process.getInExecutionTask()).isNull();
        process.updateTaskStatusToExecute("test");
    }

    @Test
    void shouldTrackTaskExecution() {
        AtomicInteger taskCount = new AtomicInteger(0);
        ZLcIDataImportOrExportProcess<String> process = new ZLcIDataImportOrExportProcess<String>() {
            @Override
            public void taskAbnormalInterrupt() {
            }

            @Override
            public void taskExecute(String task) {
                taskCount.incrementAndGet();
            }

            @Override
            public String nextTask() {
                return "task-" + taskCount.get();
            }

            @Override
            public Long getInExecutionTaskCount() {
                return (long) taskCount.get();
            }

            @Override
            public String getInExecutionTask() {
                return taskCount.get() > 0 ? "task-" + taskCount.get() : null;
            }

            @Override
            public void updateTaskStatusToExecute(String task) {
            }
        };

        process.taskExecute("task-1");
        process.taskExecute("task-2");

        assertThat(taskCount.get()).isEqualTo(2);
        assertThat(process.getInExecutionTaskCount()).isEqualTo(2L);
        assertThat(process.nextTask()).isEqualTo("task-2");
    }
}