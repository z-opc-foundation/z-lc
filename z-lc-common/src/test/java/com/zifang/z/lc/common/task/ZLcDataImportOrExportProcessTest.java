package com.zifang.z.lc.common.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataImportOrExportProcess 单元测试
 *
 * @author zifang
 */
class ZLcDataImportOrExportProcessTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcDataImportOrExportProcess.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveGenericTypeParameter() {
        assertThat(ZLcDataImportOrExportProcess.class.getTypeParameters()).hasSize(1);
    }

    @Test
    void shouldImplementAllMethods() {
        TestProcess process = new TestProcess();

        process.taskAbnormalInterrupt();
        assertThat(process.abnormalInterruptCalled.get()).isEqualTo(1);

        TestTask task = new TestTask();
        process.taskExecute(task);
        assertThat(process.executedCount.get()).isEqualTo(1);

        process.nextTask();
        assertThat(process.nextTaskCalled.get()).isEqualTo(1);

        Long count = process.getInExecutionTaskCount();
        assertThat(count).isEqualTo(0L);

        TestTask current = process.getInExecutionTask();
        assertThat(current).isNull();

        process.updateTaskStatusToExecute(task);
        assertThat(process.statusUpdateCalled.get()).isEqualTo(1);
    }

    @Test
    void shouldReturnNextTaskWhenAvailable() {
        TestProcess process = new TestProcess();
        process.nextTaskToReturn = new TestTask();

        TestTask task = process.nextTask();
        assertThat(task).isNotNull();
    }

    @Test
    void shouldReturnNullNextTask() {
        TestProcess process = new TestProcess();
        TestTask task = process.nextTask();
        assertThat(task).isNull();
    }

    // 测试用的实现
    static class TestProcess implements ZLcDataImportOrExportProcess<TestTask> {
        AtomicInteger executedCount = new AtomicInteger(0);
        AtomicInteger nextTaskCalled = new AtomicInteger(0);
        AtomicInteger statusUpdateCalled = new AtomicInteger(0);
        AtomicInteger abnormalInterruptCalled = new AtomicInteger(0);
        TestTask nextTaskToReturn = null;

        @Override
        public void taskAbnormalInterrupt() {
            abnormalInterruptCalled.incrementAndGet();
        }

        @Override
        public void taskExecute(TestTask task) {
            executedCount.incrementAndGet();
        }

        @Override
        public TestTask nextTask() {
            nextTaskCalled.incrementAndGet();
            return nextTaskToReturn;
        }

        @Override
        public Long getInExecutionTaskCount() {
            return 0L;
        }

        @Override
        public TestTask getInExecutionTask() {
            return null;
        }

        @Override
        public void updateTaskStatusToExecute(TestTask task) {
            statusUpdateCalled.incrementAndGet();
        }
    }

    // 测试用的具体Task
    static class TestTask extends ZLcBaseDataProcessTask {
    }
}