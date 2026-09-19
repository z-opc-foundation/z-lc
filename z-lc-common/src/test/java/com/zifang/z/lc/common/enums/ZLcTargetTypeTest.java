package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTargetType 单元测试
 */
class ZLcTargetTypeTest {

    @Test
    void shouldHaveTwoValues() {
        assertThat(ZLcTargetType.values()).hasSize(2);
    }

    @Test
    void shouldHaveCorrectCodes() {
        assertThat(ZLcTargetType.CAMEL_CASE.getCode()).isEqualTo("camelCase");
        assertThat(ZLcTargetType.UNDERLINE.getCode()).isEqualTo("underline");
    }

    @Test
    void shouldHaveCorrectDescriptions() {
        assertThat(ZLcTargetType.CAMEL_CASE.getDescription()).isEqualTo("驼峰");
        assertThat(ZLcTargetType.UNDERLINE.getDescription()).isEqualTo("下划线");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcTargetType.fromCode("camelCase")).isEqualTo(ZLcTargetType.CAMEL_CASE);
        assertThat(ZLcTargetType.fromCode("underline")).isEqualTo(ZLcTargetType.UNDERLINE);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcTargetType.fromCode("unknown")).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcTargetType.fromCode(null)).isNull();
    }
}