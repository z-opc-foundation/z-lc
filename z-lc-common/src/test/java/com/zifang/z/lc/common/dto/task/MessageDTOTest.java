package com.zifang.z.lc.common.dto.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageDTO 单元测试
 *
 * @author zifang
 */
class MessageDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        MessageDTO dto = new MessageDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetTaskInfo() {
        MessageDTO dto = new MessageDTO();
        TaskInfoDTO taskInfo = new TaskInfoDTO();
        dto.setTaskInfo(taskInfo);
        assertThat(dto.getTaskInfo()).isEqualTo(taskInfo);
    }

    @Test
    void shouldSetAndGetActionType() {
        MessageDTO dto = new MessageDTO();
        dto.setActionType("todo");
        assertThat(dto.getActionType()).isEqualTo("todo");
    }

    @Test
    void shouldSetAndGetAppCode() {
        MessageDTO dto = new MessageDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        MessageDTO dto = new MessageDTO();
        dto.setModelCode("model-001");
        assertThat(dto.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetPageCode() {
        MessageDTO dto = new MessageDTO();
        dto.setPageCode("page-001");
        assertThat(dto.getPageCode()).isEqualTo("page-001");
    }

    @Test
    void shouldSetAndGetPkId() {
        MessageDTO dto = new MessageDTO();
        dto.setPkId(123L);
        assertThat(dto.getPkId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetBusinessKey() {
        MessageDTO dto = new MessageDTO();
        dto.setBusinessKey("business-789");
        assertThat(dto.getBusinessKey()).isEqualTo("business-789");
    }

    @Test
    void shouldSetAndGetInitiator() {
        MessageDTO dto = new MessageDTO();
        dto.setInitiator(1001L);
        assertThat(dto.getInitiator()).isEqualTo(1001L);
    }

    @Test
    void shouldHandleNullValues() {
        MessageDTO dto = new MessageDTO();
        assertThat(dto.getTaskInfo()).isNull();
        assertThat(dto.getActionType()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
        assertThat(dto.getPageCode()).isNull();
        assertThat(dto.getPkId()).isNull();
        assertThat(dto.getBusinessKey()).isNull();
        assertThat(dto.getInitiator()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        MessageDTO dto = new MessageDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHaveCorrectConstants() {
        assertThat(MessageDTO.TODO).isEqualTo("todo");
        assertThat(MessageDTO.DONE).isEqualTo("done");
        assertThat(MessageDTO.REVOKE).isEqualTo("revoke");
    }

    @Test
    void shouldCreateTodoMessageWithFactoryMethod() {
        TaskInfoDTO taskInfo = new TaskInfoDTO();
        MessageDTO message = MessageDTO.todo(taskInfo, "app-001", "model-001", 1001L);
        
        assertThat(message.getTaskInfo()).isEqualTo(taskInfo);
        assertThat(message.getActionType()).isEqualTo("todo");
        assertThat(message.getAppCode()).isEqualTo("app-001");
        assertThat(message.getModelCode()).isEqualTo("model-001");
        assertThat(message.getInitiator()).isEqualTo(1001L);
    }

    @Test
    void shouldCreateDoneMessageWithFactoryMethod() {
        TaskInfoDTO taskInfo = new TaskInfoDTO();
        MessageDTO message = MessageDTO.done(taskInfo, "app-001", "model-001", 1001L);
        
        assertThat(message.getTaskInfo()).isEqualTo(taskInfo);
        assertThat(message.getActionType()).isEqualTo("done");
        assertThat(message.getAppCode()).isEqualTo("app-001");
        assertThat(message.getModelCode()).isEqualTo("model-001");
        assertThat(message.getInitiator()).isEqualTo(1001L);
    }

    @Test
    void shouldCreateRevokeMessageWithFactoryMethod() {
        TaskInfoDTO taskInfo = new TaskInfoDTO();
        MessageDTO message = MessageDTO.revoke(taskInfo, "app-001", "model-001", 1001L);
        
        assertThat(message.getTaskInfo()).isEqualTo(taskInfo);
        assertThat(message.getActionType()).isEqualTo("revoke");
        assertThat(message.getAppCode()).isEqualTo("app-001");
        assertThat(message.getModelCode()).isEqualTo("model-001");
        assertThat(message.getInitiator()).isEqualTo(1001L);
    }

    @Test
    void shouldSetEmptyStrings() {
        MessageDTO dto = new MessageDTO();
        dto.setActionType("");
        dto.setAppCode("");
        dto.setModelCode("");
        dto.setPageCode("");
        dto.setBusinessKey("");
        
        assertThat(dto.getActionType()).isEmpty();
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getModelCode()).isEmpty();
        assertThat(dto.getPageCode()).isEmpty();
        assertThat(dto.getBusinessKey()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        MessageDTO dto = new MessageDTO();
        dto.setActionType(null);
        dto.setAppCode(null);
        dto.setModelCode(null);
        dto.setPageCode(null);
        dto.setBusinessKey(null);
        
        assertThat(dto.getActionType()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
        assertThat(dto.getPageCode()).isNull();
        assertThat(dto.getBusinessKey()).isNull();
    }
}