package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AddSignExtendDTO 单元测试
 *
 * @author zifang
 */
class AddSignExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetWorkflowInstanceId() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setWorkflowInstanceId("wf-123");
        assertThat(dto.getWorkflowInstanceId()).isEqualTo("wf-123");
    }

    @Test
    void shouldSetAndGetTaskId() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setTaskId("task-456");
        assertThat(dto.getTaskId()).isEqualTo("task-456");
    }

    @Test
    void shouldSetAndGetSignData() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        Map<String, Object> signData = new HashMap<>();
        signData.put("key1", "value1");
        signData.put("key2", 123);
        dto.setSignData(signData);
        assertThat(dto.getSignData()).isEqualTo(signData);
    }

    @Test
    void shouldSetAndGetElectronicSignInfoDTO() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        ElectronicSignInfoExtendDTO electronicSignInfoDTO = new ElectronicSignInfoExtendDTO();
        dto.setElectronicSignInfoDTO(electronicSignInfoDTO);
        assertThat(dto.getElectronicSignInfoDTO()).isEqualTo(electronicSignInfoDTO);
    }

    @Test
    void shouldSetAndGetStaffId() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setStaffId(1001L);
        assertThat(dto.getStaffId()).isEqualTo(1001L);
    }

    @Test
    void shouldSetAndGetJobNumber() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setJobNumber("EMP001");
        assertThat(dto.getJobNumber()).isEqualTo("EMP001");
    }

    @Test
    void shouldSetAndGetClientType() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setClientType("PC");
        assertThat(dto.getClientType()).isEqualTo("PC");
    }

    @Test
    void shouldSetAndGetBusinessKey() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setBusinessKey("business-789");
        assertThat(dto.getBusinessKey()).isEqualTo("business-789");
    }

    @Test
    void shouldSetAndGetPageName() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setPageName("sign-page");
        assertThat(dto.getPageName()).isEqualTo("sign-page");
    }

    @Test
    void shouldHandleNullValues() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        assertThat(dto.getWorkflowInstanceId()).isNull();
        assertThat(dto.getTaskId()).isNull();
        assertThat(dto.getSignData()).isNull();
        assertThat(dto.getElectronicSignInfoDTO()).isNull();
        assertThat(dto.getStaffId()).isNull();
        assertThat(dto.getJobNumber()).isNull();
        assertThat(dto.getClientType()).isNull();
        assertThat(dto.getBusinessKey()).isNull();
        assertThat(dto.getPageName()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setWorkflowInstanceId("");
        dto.setTaskId("");
        dto.setJobNumber("");
        dto.setClientType("");
        dto.setBusinessKey("");
        dto.setPageName("");
        
        assertThat(dto.getWorkflowInstanceId()).isEmpty();
        assertThat(dto.getTaskId()).isEmpty();
        assertThat(dto.getJobNumber()).isEmpty();
        assertThat(dto.getClientType()).isEmpty();
        assertThat(dto.getBusinessKey()).isEmpty();
        assertThat(dto.getPageName()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setWorkflowInstanceId(null);
        dto.setTaskId(null);
        dto.setJobNumber(null);
        dto.setClientType(null);
        dto.setBusinessKey(null);
        dto.setPageName(null);
        
        assertThat(dto.getWorkflowInstanceId()).isNull();
        assertThat(dto.getTaskId()).isNull();
        assertThat(dto.getJobNumber()).isNull();
        assertThat(dto.getClientType()).isNull();
        assertThat(dto.getBusinessKey()).isNull();
        assertThat(dto.getPageName()).isNull();
    }

    @Test
    void shouldSetEmptySignData() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setSignData(new HashMap<>());
        assertThat(dto.getSignData()).isEmpty();
    }

    @Test
    void shouldSetNullSignData() {
        AddSignExtendDTO dto = new AddSignExtendDTO();
        dto.setSignData(null);
        assertThat(dto.getSignData()).isNull();
    }
}