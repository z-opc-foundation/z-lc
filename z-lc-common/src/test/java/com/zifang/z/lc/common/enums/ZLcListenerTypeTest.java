package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcListenerType 单元测试
 */
class ZLcListenerTypeTest {

    @Test
    void shouldHaveTwoValues() {
        assertThat(ZLcListenerType.values()).hasSize(2);
    }

    @Test
    void shouldHaveCorrectCodes() {
        assertThat(ZLcListenerType.SUCCESS.getCode()).isEqualTo(1);
        assertThat(ZLcListenerType.FAIL.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveCorrectDescriptions() {
        assertThat(ZLcListenerType.SUCCESS.getDesc()).isEqualTo("成功");
        assertThat(ZLcListenerType.FAIL.getDesc()).isEqualTo("失败");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcListenerType.fromCode(1)).isEqualTo(ZLcListenerType.SUCCESS);
        assertThat(ZLcListenerType.fromCode(0)).isEqualTo(ZLcListenerType.FAIL);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcListenerType.fromCode(99)).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcListenerType.fromCode(null)).isNull();
    }
}