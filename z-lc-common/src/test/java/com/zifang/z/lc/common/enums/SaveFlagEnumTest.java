package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SaveFlagEnum 单元测试
 *
 * @author zifang
 */
class SaveFlagEnumTest {

    @Test
    void shouldHaveCorrectCodeForSUCCESS() {
        assertThat(SaveFlagEnum.SUCCESS.getCode()).isEqualTo("success");
    }

    @Test
    void shouldHaveCorrectDescForSUCCESS() {
        assertThat(SaveFlagEnum.SUCCESS.getDesc()).isEqualTo("成功");
    }

    @Test
    void shouldHaveCorrectCodeForFAIL() {
        assertThat(SaveFlagEnum.FAIL.getCode()).isEqualTo("fail");
    }

    @Test
    void shouldHaveCorrectDescForFAIL() {
        assertThat(SaveFlagEnum.FAIL.getDesc()).isEqualTo("失败");
    }

    @Test
    void shouldReturnEnumByCodeForSUCCESS() {
        assertThat(SaveFlagEnum.fromCode("success")).isEqualTo(SaveFlagEnum.SUCCESS);
    }

    @Test
    void shouldReturnEnumByCodeForFAIL() {
        assertThat(SaveFlagEnum.fromCode("fail")).isEqualTo(SaveFlagEnum.FAIL);
    }

    @Test
    void shouldReturnEnumByCodeCaseInsensitive() {
        assertThat(SaveFlagEnum.fromCode("SUCCESS")).isEqualTo(SaveFlagEnum.SUCCESS);
        assertThat(SaveFlagEnum.fromCode("FAIL")).isEqualTo(SaveFlagEnum.FAIL);
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        assertThat(SaveFlagEnum.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullCode() {
        assertThat(SaveFlagEnum.fromCode(null)).isNull();
    }

    @Test
    void shouldReturnTrueForIsSuccessWhenCodeIsSuccess() {
        assertThat(SaveFlagEnum.isSuccess("success")).isTrue();
    }

    @Test
    void shouldReturnFalseForIsSuccessWhenCodeIsFail() {
        assertThat(SaveFlagEnum.isSuccess("fail")).isFalse();
    }

    @Test
    void shouldReturnFalseForIsSuccessWhenCodeIsNull() {
        assertThat(SaveFlagEnum.isSuccess(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForIsSuccessWhenCodeIsUnknown() {
        assertThat(SaveFlagEnum.isSuccess("unknown")).isFalse();
    }

    @Test
    void shouldReturnTrueForIsFailWhenCodeIsFail() {
        assertThat(SaveFlagEnum.isFail("fail")).isTrue();
    }

    @Test
    void shouldReturnFalseForIsFailWhenCodeIsSuccess() {
        assertThat(SaveFlagEnum.isFail("success")).isFalse();
    }

    @Test
    void shouldReturnFalseForIsFailWhenCodeIsNull() {
        assertThat(SaveFlagEnum.isFail(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForIsFailWhenCodeIsUnknown() {
        assertThat(SaveFlagEnum.isFail("unknown")).isFalse();
    }

    @Test
    void shouldHaveTwoValues() {
        assertThat(SaveFlagEnum.values()).hasSize(2);
    }
}