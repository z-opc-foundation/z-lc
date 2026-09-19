package com.zifang.z.lc.common.bpmn.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcUserTaskInfo 单元测试
 */
class ZLcUserTaskInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo();
        assertThat(info).isNotNull();
        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getAssignee()).isNull();
        assertThat(info.getDocumentation()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo("task1", "Approve", "alice", "Approval step");
        assertThat(info.getId()).isEqualTo("task1");
        assertThat(info.getName()).isEqualTo("Approve");
        assertThat(info.getAssignee()).isEqualTo("alice");
        assertThat(info.getDocumentation()).isEqualTo("Approval step");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo();
        assertThat(info).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportSetters() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo();
        info.setId("id1");
        info.setName("name1");
        info.setAssignee("user1");
        info.setDocumentation("doc1");

        assertThat(info.getId()).isEqualTo("id1");
        assertThat(info.getName()).isEqualTo("name1");
        assertThat(info.getAssignee()).isEqualTo("user1");
        assertThat(info.getDocumentation()).isEqualTo("doc1");
    }

    @Test
    void toStringShouldIncludeIdNameAssignee() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo("t1", "Task", "alice", null);
        assertThat(info.toString())
                .contains("t1")
                .contains("Task")
                .contains("alice");
    }

    @Test
    void shouldHandleNullAssignee() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo("t1", "Task", null, null);
        assertThat(info.getAssignee()).isNull();
    }

    @Test
    void shouldHandleEmptyAssignee() {
        ZLcUserTaskInfo info = new ZLcUserTaskInfo("t1", "Task", "", null);
        assertThat(info.getAssignee()).isEqualTo("");
    }
}