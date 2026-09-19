package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcListenerSendResult 单元测试
 *
 * @author zifang
 */
class ZLcListenerSendResultTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcListenerSendResult.values()).hasSize(2);
    }

    @Test
    void shouldHaveSuccessCode() {
        assertThat(ZLcListenerSendResult.SUCCESS.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveSuccessDesc() {
        assertThat(ZLcListenerSendResult.SUCCESS.getDesc()).isEqualTo("成功");
    }

    @Test
    void shouldHaveFailCode() {
        assertThat(ZLcListenerSendResult.FAIL.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveFailDesc() {
        assertThat(ZLcListenerSendResult.FAIL.getDesc()).isEqualTo("失败");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcListenerSendResult.fromCode(1)).isEqualTo(ZLcListenerSendResult.SUCCESS);
        assertThat(ZLcListenerSendResult.fromCode(0)).isEqualTo(ZLcListenerSendResult.FAIL);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcListenerSendResult.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcListenerSendResult.fromCode(null)).isNull();
    }

    @Test
    void shouldIsSuccessReturnTrueForSuccess() {
        assertThat(ZLcListenerSendResult.SUCCESS.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForFail() {
        assertThat(ZLcListenerSendResult.FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcListenerSendResult.valueOf("SUCCESS")).isEqualTo(ZLcListenerSendResult.SUCCESS);
        assertThat(ZLcListenerSendResult.valueOf("FAIL")).isEqualTo(ZLcListenerSendResult.FAIL);
    }
}
