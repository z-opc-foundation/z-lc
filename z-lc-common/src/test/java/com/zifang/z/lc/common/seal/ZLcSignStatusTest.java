package com.zifang.z.lc.common.seal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSignStatus 单元测试
 *
 * @author zifang
 */
class ZLcSignStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcSignStatus.values()).hasSize(4);
    }

    @Test
    void shouldHaveSigningCode() {
        assertThat(ZLcSignStatus.SIGNING.getCode()).isEqualTo("SIGNING");
    }

    @Test
    void shouldHaveSigningDescription() {
        assertThat(ZLcSignStatus.SIGNING.getDescription()).isEqualTo("签章中");
    }

    @Test
    void shouldHaveSuccessCode() {
        assertThat(ZLcSignStatus.SUCCESS.getCode()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldHaveSuccessDescription() {
        assertThat(ZLcSignStatus.SUCCESS.getDescription()).isEqualTo("签章成功");
    }

    @Test
    void shouldHaveFailCode() {
        assertThat(ZLcSignStatus.FAIL.getCode()).isEqualTo("FAIL");
    }

    @Test
    void shouldHaveFailDescription() {
        assertThat(ZLcSignStatus.FAIL.getDescription()).isEqualTo("签章失败");
    }

    @Test
    void shouldHaveInvalidCode() {
        assertThat(ZLcSignStatus.INVALID.getCode()).isEqualTo("INVALID");
    }

    @Test
    void shouldHaveInvalidDescription() {
        assertThat(ZLcSignStatus.INVALID.getDescription()).isEqualTo("无效签章记录");
    }

    @Test
    void shouldOfCode() {
        assertThat(ZLcSignStatus.ofCode("SIGNING")).isEqualTo(ZLcSignStatus.SIGNING);
        assertThat(ZLcSignStatus.ofCode("SUCCESS")).isEqualTo(ZLcSignStatus.SUCCESS);
        assertThat(ZLcSignStatus.ofCode("FAIL")).isEqualTo(ZLcSignStatus.FAIL);
        assertThat(ZLcSignStatus.ofCode("INVALID")).isEqualTo(ZLcSignStatus.INVALID);
    }

    @Test
    void shouldReturnNullForUnknownOfCode() {
        assertThat(ZLcSignStatus.ofCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullOfCode() {
        assertThat(ZLcSignStatus.ofCode(null)).isNull();
    }

    @Test
    void shouldGetDescriptionByCode() {
        assertThat(ZLcSignStatus.getDescriptionByCode("SIGNING")).isEqualTo("签章中");
        assertThat(ZLcSignStatus.getDescriptionByCode("SUCCESS")).isEqualTo("签章成功");
        assertThat(ZLcSignStatus.getDescriptionByCode("FAIL")).isEqualTo("签章失败");
        assertThat(ZLcSignStatus.getDescriptionByCode("INVALID")).isEqualTo("无效签章记录");
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForUnknown() {
        assertThat(ZLcSignStatus.getDescriptionByCode("unknown")).isNull();
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForNull() {
        assertThat(ZLcSignStatus.getDescriptionByCode(null)).isNull();
    }

    @Test
    void shouldIsTerminalReturnTrueForSuccess() {
        assertThat(ZLcSignStatus.SUCCESS.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForFail() {
        assertThat(ZLcSignStatus.FAIL.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForInvalid() {
        assertThat(ZLcSignStatus.INVALID.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnFalseForSigning() {
        assertThat(ZLcSignStatus.SIGNING.isTerminal()).isFalse();
    }

    @Test
    void shouldIsSuccessReturnTrueForSuccess() {
        assertThat(ZLcSignStatus.SUCCESS.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForFail() {
        assertThat(ZLcSignStatus.FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcSignStatus.valueOf("SIGNING")).isEqualTo(ZLcSignStatus.SIGNING);
        assertThat(ZLcSignStatus.valueOf("SUCCESS")).isEqualTo(ZLcSignStatus.SUCCESS);
        assertThat(ZLcSignStatus.valueOf("FAIL")).isEqualTo(ZLcSignStatus.FAIL);
        assertThat(ZLcSignStatus.valueOf("INVALID")).isEqualTo(ZLcSignStatus.INVALID);
    }
}
