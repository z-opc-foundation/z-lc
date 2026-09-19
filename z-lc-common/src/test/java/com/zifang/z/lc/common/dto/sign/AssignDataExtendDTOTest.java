package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssignDataExtendDTO 单元测试
 *
 * @author zifang
 */
class AssignDataExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetWorkflowInstanceId() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setWorkflowInstanceId("wf-123");
        assertThat(dto.getWorkflowInstanceId()).isEqualTo("wf-123");
    }

    @Test
    void shouldSetAndGetTaskId() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setTaskId("task-456");
        assertThat(dto.getTaskId()).isEqualTo("task-456");
    }

    @Test
    void shouldSetAndGetFormData() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        Map<String, Object> formData = new HashMap<>();
        formData.put("field1", "value1");
        formData.put("field2", 123);
        dto.setFormData(formData);
        assertThat(dto.getFormData()).isEqualTo(formData);
    }

    @Test
    void shouldHandleNullValues() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        assertThat(dto.getWorkflowInstanceId()).isNull();
        assertThat(dto.getTaskId()).isNull();
        assertThat(dto.getFormData()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setWorkflowInstanceId("");
        dto.setTaskId("");
        
        assertThat(dto.getWorkflowInstanceId()).isEmpty();
        assertThat(dto.getTaskId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setWorkflowInstanceId(null);
        dto.setTaskId(null);
        
        assertThat(dto.getWorkflowInstanceId()).isNull();
        assertThat(dto.getTaskId()).isNull();
    }

    @Test
    void shouldSetEmptyFormData() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setFormData(new HashMap<>());
        assertThat(dto.getFormData()).isEmpty();
    }

    @Test
    void shouldSetNullFormData() {
        AssignDataExtendDTO dto = new AssignDataExtendDTO();
        dto.setFormData(null);
        assertThat(dto.getFormData()).isNull();
    }
}