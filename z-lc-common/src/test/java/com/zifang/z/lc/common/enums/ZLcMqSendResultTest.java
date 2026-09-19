package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcMqSendResult 单元测试
 *
 * @author zifang
 */
class ZLcMqSendResultTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcMqSendResult.values()).hasSize(2);
    }

    @Test
    void shouldHaveSuccessCode() {
        assertThat(ZLcMqSendResult.SUCCESS.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveSuccessDesc() {
        assertThat(ZLcMqSendResult.SUCCESS.getDesc()).isEqualTo("成功");
    }

    @Test
    void shouldHaveFailCode() {
        assertThat(ZLcMqSendResult.FAIL.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveFailDesc() {
        assertThat(ZLcMqSendResult.FAIL.getDesc()).isEqualTo("失败");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcMqSendResult.fromCode(1)).isEqualTo(ZLcMqSendResult.SUCCESS);
        assertThat(ZLcMqSendResult.fromCode(0)).isEqualTo(ZLcMqSendResult.FAIL);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcMqSendResult.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcMqSendResult.fromCode(null)).isNull();
    }

    @Test
    void shouldIsSuccessReturnTrueForSuccess() {
        assertThat(ZLcMqSendResult.SUCCESS.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForFail() {
        assertThat(ZLcMqSendResult.FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcMqSendResult.valueOf("SUCCESS")).isEqualTo(ZLcMqSendResult.SUCCESS);
        assertThat(ZLcMqSendResult.valueOf("FAIL")).isEqualTo(ZLcMqSendResult.FAIL);
    }
}
