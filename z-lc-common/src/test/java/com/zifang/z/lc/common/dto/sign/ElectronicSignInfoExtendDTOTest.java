package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElectronicSignInfoExtendDTO 单元测试
 *
 * @author zifang
 */
class ElectronicSignInfoExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSignDataId() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignDataId("sign-123");
        assertThat(dto.getSignDataId()).isEqualTo("sign-123");
    }

    @Test
    void shouldSetAndGetTimeStampValue() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setTimeStampValue("2023-01-01 12:00:00");
        assertThat(dto.getTimeStampValue()).isEqualTo("2023-01-01 12:00:00");
    }

    @Test
    void shouldSetAndGetSignResult() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignResult("base64-sign-result");
        assertThat(dto.getSignResult()).isEqualTo("base64-sign-result");
    }

    @Test
    void shouldSetAndGetSignCert() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignCert("base64-cert");
        assertThat(dto.getSignCert()).isEqualTo("base64-cert");
    }

    @Test
    void shouldSetAndGetJobStatus() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setJobStatus("FINISH");
        assertThat(dto.getJobStatus()).isEqualTo("FINISH");
    }

    @Test
    void shouldSetAndGetMsspId() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setMsspId("user-456");
        assertThat(dto.getMsspId()).isEqualTo("user-456");
    }

    @Test
    void shouldSetAndGetSignImage() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignImage("base64-image");
        assertThat(dto.getSignImage()).isEqualTo("base64-image");
    }

    @Test
    void shouldSetAndGetSignPassword() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignPassword("password123");
        assertThat(dto.getSignPassword()).isEqualTo("password123");
    }

    @Test
    void shouldHandleNullValues() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getTimeStampValue()).isNull();
        assertThat(dto.getSignResult()).isNull();
        assertThat(dto.getSignCert()).isNull();
        assertThat(dto.getJobStatus()).isNull();
        assertThat(dto.getMsspId()).isNull();
        assertThat(dto.getSignImage()).isNull();
        assertThat(dto.getSignPassword()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldReturnTrueForIsSignedWhenJobStatusIsFINISH() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setJobStatus("FINISH");
        assertThat(dto.isSigned()).isTrue();
    }

    @Test
    void shouldReturnFalseForIsSignedWhenJobStatusIsNotFINISH() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setJobStatus("UNSIGN");
        assertThat(dto.isSigned()).isFalse();
    }

    @Test
    void shouldReturnFalseForIsSignedWhenJobStatusIsNull() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setJobStatus(null);
        assertThat(dto.isSigned()).isFalse();
    }

    @Test
    void shouldReturnTrueForIsSignedWhenJobStatusIsCaseInsensitive() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setJobStatus("finish");
        assertThat(dto.isSigned()).isTrue();
    }

    @Test
    void shouldSetEmptyStrings() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignDataId("");
        dto.setTimeStampValue("");
        dto.setSignResult("");
        dto.setSignCert("");
        dto.setJobStatus("");
        dto.setMsspId("");
        dto.setSignImage("");
        dto.setSignPassword("");
        
        assertThat(dto.getSignDataId()).isEmpty();
        assertThat(dto.getTimeStampValue()).isEmpty();
        assertThat(dto.getSignResult()).isEmpty();
        assertThat(dto.getSignCert()).isEmpty();
        assertThat(dto.getJobStatus()).isEmpty();
        assertThat(dto.getMsspId()).isEmpty();
        assertThat(dto.getSignImage()).isEmpty();
        assertThat(dto.getSignPassword()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ElectronicSignInfoExtendDTO dto = new ElectronicSignInfoExtendDTO();
        dto.setSignDataId(null);
        dto.setTimeStampValue(null);
        dto.setSignResult(null);
        dto.setSignCert(null);
        dto.setJobStatus(null);
        dto.setMsspId(null);
        dto.setSignImage(null);
        dto.setSignPassword(null);
        
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getTimeStampValue()).isNull();
        assertThat(dto.getSignResult()).isNull();
        assertThat(dto.getSignCert()).isNull();
        assertThat(dto.getJobStatus()).isNull();
        assertThat(dto.getMsspId()).isNull();
        assertThat(dto.getSignImage()).isNull();
        assertThat(dto.getSignPassword()).isNull();
    }
}