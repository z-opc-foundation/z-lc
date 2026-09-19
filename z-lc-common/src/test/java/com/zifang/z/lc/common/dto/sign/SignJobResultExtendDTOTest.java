package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SignJobResultExtendDTO 单元测试
 *
 * @author zifang
 */
class SignJobResultExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetAutoSignFlag() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        dto.setAutoSignFlag(true);
        assertThat(dto.getAutoSignFlag()).isTrue();
    }

    @Test
    void shouldSetAndGetAddSignJobResultDTO() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        AddSignJobResultExtendDTO addSignJobResultDTO = new AddSignJobResultExtendDTO();
        dto.setAddSignJobResultDTO(addSignJobResultDTO);
        assertThat(dto.getAddSignJobResultDTO()).isEqualTo(addSignJobResultDTO);
    }

    @Test
    void shouldSetAndGetElectronicSignInfoDTO() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        ElectronicSignInfoExtendDTO electronicSignInfoDTO = new ElectronicSignInfoExtendDTO();
        dto.setElectronicSignInfoDTO(electronicSignInfoDTO);
        assertThat(dto.getElectronicSignInfoDTO()).isEqualTo(electronicSignInfoDTO);
    }

    @Test
    void shouldHandleNullValues() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        assertThat(dto.getAutoSignFlag()).isNull();
        assertThat(dto.getAddSignJobResultDTO()).isNull();
        assertThat(dto.getElectronicSignInfoDTO()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldResolveFinalSignInfoWhenAutoSignFlagIsTrue() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        dto.setAutoSignFlag(true);
        ElectronicSignInfoExtendDTO electronicSignInfoDTO = new ElectronicSignInfoExtendDTO();
        dto.setElectronicSignInfoDTO(electronicSignInfoDTO);
        
        ElectronicSignInfoExtendDTO result = dto.resolveFinalSignInfo();
        assertThat(result).isEqualTo(electronicSignInfoDTO);
    }

    @Test
    void shouldResolveFinalSignInfoWhenAutoSignFlagIsFalse() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        dto.setAutoSignFlag(false);
        ElectronicSignInfoExtendDTO electronicSignInfoDTO = new ElectronicSignInfoExtendDTO();
        dto.setElectronicSignInfoDTO(electronicSignInfoDTO);
        
        ElectronicSignInfoExtendDTO result = dto.resolveFinalSignInfo();
        assertThat(result).isNull();
    }

    @Test
    void shouldResolveFinalSignInfoWhenAutoSignFlagIsNull() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        dto.setAutoSignFlag(null);
        ElectronicSignInfoExtendDTO electronicSignInfoDTO = new ElectronicSignInfoExtendDTO();
        dto.setElectronicSignInfoDTO(electronicSignInfoDTO);
        
        ElectronicSignInfoExtendDTO result = dto.resolveFinalSignInfo();
        assertThat(result).isNull();
    }

    @Test
    void shouldResolveFinalSignInfoWhenElectronicSignInfoDTOIsNull() {
        SignJobResultExtendDTO dto = new SignJobResultExtendDTO();
        dto.setAutoSignFlag(true);
        dto.setElectronicSignInfoDTO(null);
        
        ElectronicSignInfoExtendDTO result = dto.resolveFinalSignInfo();
        assertThat(result).isNull();
    }
}