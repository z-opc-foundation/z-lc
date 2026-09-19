package com.zifang.z.lc.common.seal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcVerifyStatus 单元测试
 *
 * @author zifang
 */
class ZLcVerifyStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcVerifyStatus.values()).hasSize(4);
    }

    @Test
    void shouldHaveUnverifiedCode() {
        assertThat(ZLcVerifyStatus.UNVERIFIED.getCode()).isEqualTo("UNVERIFIED");
    }

    @Test
    void shouldHaveUnverifiedDescription() {
        assertThat(ZLcVerifyStatus.UNVERIFIED.getDescription()).isEqualTo("未验章");
    }

    @Test
    void shouldHaveVerifyingCode() {
        assertThat(ZLcVerifyStatus.VERIFYING.getCode()).isEqualTo("VERIFYING");
    }

    @Test
    void shouldHaveVerifyingDescription() {
        assertThat(ZLcVerifyStatus.VERIFYING.getDescription()).isEqualTo("验章中");
    }

    @Test
    void shouldHaveSuccessCode() {
        assertThat(ZLcVerifyStatus.SUCCESS.getCode()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldHaveSuccessDescription() {
        assertThat(ZLcVerifyStatus.SUCCESS.getDescription()).isEqualTo("验章成功");
    }

    @Test
    void shouldHaveFailCode() {
        assertThat(ZLcVerifyStatus.FAIL.getCode()).isEqualTo("FAIL");
    }

    @Test
    void shouldHaveFailDescription() {
        assertThat(ZLcVerifyStatus.FAIL.getDescription()).isEqualTo("验章失败");
    }

    @Test
    void shouldOfCode() {
        assertThat(ZLcVerifyStatus.ofCode("UNVERIFIED")).isEqualTo(ZLcVerifyStatus.UNVERIFIED);
        assertThat(ZLcVerifyStatus.ofCode("VERIFYING")).isEqualTo(ZLcVerifyStatus.VERIFYING);
        assertThat(ZLcVerifyStatus.ofCode("SUCCESS")).isEqualTo(ZLcVerifyStatus.SUCCESS);
        assertThat(ZLcVerifyStatus.ofCode("FAIL")).isEqualTo(ZLcVerifyStatus.FAIL);
    }

    @Test
    void shouldReturnNullForUnknownOfCode() {
        assertThat(ZLcVerifyStatus.ofCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullOfCode() {
        assertThat(ZLcVerifyStatus.ofCode(null)).isNull();
    }

    @Test
    void shouldGetDescriptionByCode() {
        assertThat(ZLcVerifyStatus.getDescriptionByCode("UNVERIFIED")).isEqualTo("未验章");
        assertThat(ZLcVerifyStatus.getDescriptionByCode("VERIFYING")).isEqualTo("验章中");
        assertThat(ZLcVerifyStatus.getDescriptionByCode("SUCCESS")).isEqualTo("验章成功");
        assertThat(ZLcVerifyStatus.getDescriptionByCode("FAIL")).isEqualTo("验章失败");
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForUnknown() {
        assertThat(ZLcVerifyStatus.getDescriptionByCode("unknown")).isNull();
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForNull() {
        assertThat(ZLcVerifyStatus.getDescriptionByCode(null)).isNull();
    }

    @Test
    void shouldIsTerminalReturnTrueForSuccess() {
        assertThat(ZLcVerifyStatus.SUCCESS.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForFail() {
        assertThat(ZLcVerifyStatus.FAIL.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnFalseForUnverified() {
        assertThat(ZLcVerifyStatus.UNVERIFIED.isTerminal()).isFalse();
    }

    @Test
    void shouldIsTerminalReturnFalseForVerifying() {
        assertThat(ZLcVerifyStatus.VERIFYING.isTerminal()).isFalse();
    }

    @Test
    void shouldIsSuccessReturnTrueForSuccess() {
        assertThat(ZLcVerifyStatus.SUCCESS.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForFail() {
        assertThat(ZLcVerifyStatus.FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcVerifyStatus.valueOf("UNVERIFIED")).isEqualTo(ZLcVerifyStatus.UNVERIFIED);
        assertThat(ZLcVerifyStatus.valueOf("VERIFYING")).isEqualTo(ZLcVerifyStatus.VERIFYING);
        assertThat(ZLcVerifyStatus.valueOf("SUCCESS")).isEqualTo(ZLcVerifyStatus.SUCCESS);
        assertThat(ZLcVerifyStatus.valueOf("FAIL")).isEqualTo(ZLcVerifyStatus.FAIL);
    }
}
