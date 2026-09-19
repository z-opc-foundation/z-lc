package com.zifang.z.lc.common.dto.task;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskInfoDTO 单元测试
 *
 * @author zifang
 */
class TaskInfoDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        TaskInfoDTO dto = new TaskInfoDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        List<Long> assigneeList = Arrays.asList(1001L, 1002L);
        TaskInfoDTO dto = new TaskInfoDTO(
            "task-123",
            assigneeList,
            "taskDefKey-456",
            "审批任务",
            "process-789",
            "agree"
        );
        
        assertThat(dto.getTaskId()).isEqualTo("task-123");
        assertThat(dto.getAssigneeList()).isEqualTo(assigneeList);
        assertThat(dto.getTaskDefinitionKey()).isEqualTo("taskDefKey-456");
        assertThat(dto.getCurrentTaskName()).isEqualTo("审批任务");
        assertThat(dto.getProcessInstanceId()).isEqualTo("process-789");
        assertThat(dto.getOperateType()).isEqualTo("agree");
    }

    @Test
    void shouldSetAndGetTaskId() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setTaskId("task-123");
        assertThat(dto.getTaskId()).isEqualTo("task-123");
    }

    @Test
    void shouldSetAndGetAssigneeList() {
        TaskInfoDTO dto = new TaskInfoDTO();
        List<Long> assigneeList = Arrays.asList(1001L, 1002L);
        dto.setAssigneeList(assigneeList);
        assertThat(dto.getAssigneeList()).isEqualTo(assigneeList);
    }

    @Test
    void shouldSetAndGetTaskDefinitionKey() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setTaskDefinitionKey("taskDefKey-456");
        assertThat(dto.getTaskDefinitionKey()).isEqualTo("taskDefKey-456");
    }

    @Test
    void shouldSetAndGetCurrentTaskName() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setCurrentTaskName("审批任务");
        assertThat(dto.getCurrentTaskName()).isEqualTo("审批任务");
    }

    @Test
    void shouldSetAndGetProcessInstanceId() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setProcessInstanceId("process-789");
        assertThat(dto.getProcessInstanceId()).isEqualTo("process-789");
    }

    @Test
    void shouldSetAndGetOperateType() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setOperateType("agree");
        assertThat(dto.getOperateType()).isEqualTo("agree");
    }

    @Test
    void shouldHandleNullValues() {
        TaskInfoDTO dto = new TaskInfoDTO();
        assertThat(dto.getTaskId()).isNull();
        assertThat(dto.getAssigneeList()).isNull();
        assertThat(dto.getTaskDefinitionKey()).isNull();
        assertThat(dto.getCurrentTaskName()).isNull();
        assertThat(dto.getProcessInstanceId()).isNull();
        assertThat(dto.getOperateType()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        TaskInfoDTO dto = new TaskInfoDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setTaskId("");
        dto.setTaskDefinitionKey("");
        dto.setCurrentTaskName("");
        dto.setProcessInstanceId("");
        dto.setOperateType("");
        
        assertThat(dto.getTaskId()).isEmpty();
        assertThat(dto.getTaskDefinitionKey()).isEmpty();
        assertThat(dto.getCurrentTaskName()).isEmpty();
        assertThat(dto.getProcessInstanceId()).isEmpty();
        assertThat(dto.getOperateType()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setTaskId(null);
        dto.setTaskDefinitionKey(null);
        dto.setCurrentTaskName(null);
        dto.setProcessInstanceId(null);
        dto.setOperateType(null);
        
        assertThat(dto.getTaskId()).isNull();
        assertThat(dto.getTaskDefinitionKey()).isNull();
        assertThat(dto.getCurrentTaskName()).isNull();
        assertThat(dto.getProcessInstanceId()).isNull();
        assertThat(dto.getOperateType()).isNull();
    }

    @Test
    void shouldSetEmptyAssigneeList() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setAssigneeList(Arrays.asList());
        assertThat(dto.getAssigneeList()).isEmpty();
    }

    @Test
    void shouldSetNullAssigneeList() {
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setAssigneeList(null);
        assertThat(dto.getAssigneeList()).isNull();
    }
}