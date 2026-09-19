package com.zifang.z.lc.common.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBaseDataProcessTask 单元测试
 *
 * @author zifang
 */
class ZLcBaseDataProcessTaskTest {

    @Test
    void shouldBeAbstract() {
        assertThat(java.lang.reflect.Modifier.isAbstract(ZLcBaseDataProcessTask.class.getModifiers())).isTrue();
    }

    @Test
    void shouldSetAndGetId() {
        TestTask task = new TestTask();
        task.setId(100L);
        assertThat(task.getId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetTaskType() {
        TestTask task = new TestTask();
        task.setTaskType("IMPORT");
        assertThat(task.getTaskType()).isEqualTo("IMPORT");
    }

    @Test
    void shouldSetAndGetTaskStatus() {
        TestTask task = new TestTask();
        task.setTaskStatus("RUNNING");
        assertThat(task.getTaskStatus()).isEqualTo("RUNNING");
    }

    @Test
    void shouldHandleNullValues() {
        TestTask task = new TestTask();
        assertThat(task.getId()).isNull();
        assertThat(task.getTaskType()).isNull();
        assertThat(task.getTaskStatus()).isNull();
    }

    // 测试用的具体子类
    static class TestTask extends ZLcBaseDataProcessTask {
    }
}