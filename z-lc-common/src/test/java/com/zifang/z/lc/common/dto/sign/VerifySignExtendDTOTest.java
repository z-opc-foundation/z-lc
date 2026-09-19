package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerifySignExtendDTO 单元测试
 *
 * @author zifang
 */
class VerifySignExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSignDataId() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setSignDataId("sign-123");
        assertThat(dto.getSignDataId()).isEqualTo("sign-123");
    }

    @Test
    void shouldSetAndGetSignResult() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setSignResult("base64-sign-result");
        assertThat(dto.getSignResult()).isEqualTo("base64-sign-result");
    }

    @Test
    void shouldSetAndGetSignCert() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setSignCert("base64-cert");
        assertThat(dto.getSignCert()).isEqualTo("base64-cert");
    }

    @Test
    void shouldSetAndGetTimeStampValue() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setTimeStampValue("2023-01-01 12:00:00");
        assertThat(dto.getTimeStampValue()).isEqualTo("2023-01-01 12:00:00");
    }

    @Test
    void shouldSetAndGetOriginalDataHash() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setOriginalDataHash("hash-value");
        assertThat(dto.getOriginalDataHash()).isEqualTo("hash-value");
    }

    @Test
    void shouldHandleNullValues() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getSignResult()).isNull();
        assertThat(dto.getSignCert()).isNull();
        assertThat(dto.getTimeStampValue()).isNull();
        assertThat(dto.getOriginalDataHash()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setSignDataId("");
        dto.setSignResult("");
        dto.setSignCert("");
        dto.setTimeStampValue("");
        dto.setOriginalDataHash("");
        
        assertThat(dto.getSignDataId()).isEmpty();
        assertThat(dto.getSignResult()).isEmpty();
        assertThat(dto.getSignCert()).isEmpty();
        assertThat(dto.getTimeStampValue()).isEmpty();
        assertThat(dto.getOriginalDataHash()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        VerifySignExtendDTO dto = new VerifySignExtendDTO();
        dto.setSignDataId(null);
        dto.setSignResult(null);
        dto.setSignCert(null);
        dto.setTimeStampValue(null);
        dto.setOriginalDataHash(null);
        
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getSignResult()).isNull();
        assertThat(dto.getSignCert()).isNull();
        assertThat(dto.getTimeStampValue()).isNull();
        assertThat(dto.getOriginalDataHash()).isNull();
    }
}