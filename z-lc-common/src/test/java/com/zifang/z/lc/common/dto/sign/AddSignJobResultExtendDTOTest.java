package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AddSignJobResultExtendDTO 单元测试
 *
 * @author zifang
 */
class AddSignJobResultExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSignDataId() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setSignDataId("sign-123");
        assertThat(dto.getSignDataId()).isEqualTo("sign-123");
    }

    @Test
    void shouldSetAndGetQrCode() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setQrCode("base64-encoded-qr");
        assertThat(dto.getQrCode()).isEqualTo("base64-encoded-qr");
    }

    @Test
    void shouldSetAndGetMsspId() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setMsspId("user-456");
        assertThat(dto.getMsspId()).isEqualTo("user-456");
    }

    @Test
    void shouldSetAndGetAppid() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setAppid("app-789");
        assertThat(dto.getAppid()).isEqualTo("app-789");
    }

    @Test
    void shouldSetAndGetSerUrl() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setSerUrl("https://ca.example.com");
        assertThat(dto.getSerUrl()).isEqualTo("https://ca.example.com");
    }

    @Test
    void shouldSetAndGetLinkType() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType("1");
        assertThat(dto.getLinkType()).isEqualTo("1");
    }

    @Test
    void shouldSetAndGetUrlLink() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setUrlLink("https://miniprogram.example.com");
        assertThat(dto.getUrlLink()).isEqualTo("https://miniprogram.example.com");
    }

    @Test
    void shouldHandleNullValues() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getQrCode()).isNull();
        assertThat(dto.getMsspId()).isNull();
        assertThat(dto.getAppid()).isNull();
        assertThat(dto.getSerUrl()).isNull();
        assertThat(dto.getLinkType()).isNull();
        assertThat(dto.getUrlLink()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldReturnTrueForQrCodeModeWhenLinkTypeIs1() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType("1");
        assertThat(dto.isQrCodeMode()).isTrue();
    }

    @Test
    void shouldReturnFalseForQrCodeModeWhenLinkTypeIsNot1() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType("2");
        assertThat(dto.isQrCodeMode()).isFalse();
    }

    @Test
    void shouldReturnFalseForQrCodeModeWhenLinkTypeIsNull() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType(null);
        assertThat(dto.isQrCodeMode()).isFalse();
    }

    @Test
    void shouldReturnTrueForMiniProgramModeWhenLinkTypeIs2() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType("2");
        assertThat(dto.isMiniProgramMode()).isTrue();
    }

    @Test
    void shouldReturnFalseForMiniProgramModeWhenLinkTypeIsNot2() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType("1");
        assertThat(dto.isMiniProgramMode()).isFalse();
    }

    @Test
    void shouldReturnFalseForMiniProgramModeWhenLinkTypeIsNull() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setLinkType(null);
        assertThat(dto.isMiniProgramMode()).isFalse();
    }

    @Test
    void shouldSetEmptyStrings() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setSignDataId("");
        dto.setQrCode("");
        dto.setMsspId("");
        dto.setAppid("");
        dto.setSerUrl("");
        dto.setLinkType("");
        dto.setUrlLink("");
        
        assertThat(dto.getSignDataId()).isEmpty();
        assertThat(dto.getQrCode()).isEmpty();
        assertThat(dto.getMsspId()).isEmpty();
        assertThat(dto.getAppid()).isEmpty();
        assertThat(dto.getSerUrl()).isEmpty();
        assertThat(dto.getLinkType()).isEmpty();
        assertThat(dto.getUrlLink()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AddSignJobResultExtendDTO dto = new AddSignJobResultExtendDTO();
        dto.setSignDataId(null);
        dto.setQrCode(null);
        dto.setMsspId(null);
        dto.setAppid(null);
        dto.setSerUrl(null);
        dto.setLinkType(null);
        dto.setUrlLink(null);
        
        assertThat(dto.getSignDataId()).isNull();
        assertThat(dto.getQrCode()).isNull();
        assertThat(dto.getMsspId()).isNull();
        assertThat(dto.getAppid()).isNull();
        assertThat(dto.getSerUrl()).isNull();
        assertThat(dto.getLinkType()).isNull();
        assertThat(dto.getUrlLink()).isNull();
    }
}