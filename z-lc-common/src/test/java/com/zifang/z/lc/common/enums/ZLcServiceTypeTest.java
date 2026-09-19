package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcServiceType 单元测试
 */
class ZLcServiceTypeTest {

    @Test
    void shouldHaveTwoValues() {
        assertThat(ZLcServiceType.values()).hasSize(2);
    }

    @Test
    void shouldHaveCorrectCodes() {
        assertThat(ZLcServiceType.HTTP.getCode()).isEqualTo("HTTP");
        assertThat(ZLcServiceType.OPEN_PLATFORM.getCode()).isEqualTo("OPEN_PLATFORM");
    }

    @Test
    void shouldHaveCorrectDescriptions() {
        assertThat(ZLcServiceType.HTTP.getDescription()).isEqualTo("HTTP接口");
        assertThat(ZLcServiceType.OPEN_PLATFORM.getDescription()).isEqualTo("开放平台接口");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcServiceType.fromCode("HTTP")).isEqualTo(ZLcServiceType.HTTP);
        assertThat(ZLcServiceType.fromCode("OPEN_PLATFORM")).isEqualTo(ZLcServiceType.OPEN_PLATFORM);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcServiceType.fromCode("unknown")).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcServiceType.fromCode(null)).isNull();
    }
}