package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLinkType 单元测试
 */
class ZLcLinkTypeTest {

    @Test
    void shouldHaveTwoValues() {
        assertThat(ZLcLinkType.values()).hasSize(2);
    }

    @Test
    void shouldHaveCorrectTypes() {
        assertThat(ZLcLinkType.QR_CODE.getType()).isEqualTo("qrCode");
        assertThat(ZLcLinkType.URL_LINK.getType()).isEqualTo("urlLink");
    }

    @Test
    void shouldHaveCorrectNames() {
        assertThat(ZLcLinkType.QR_CODE.getName()).isEqualTo("二维码");
        assertThat(ZLcLinkType.URL_LINK.getName()).isEqualTo("url连接");
    }

    @Test
    void fromTypeShouldReturnMatching() {
        assertThat(ZLcLinkType.fromType("qrCode")).isEqualTo(ZLcLinkType.QR_CODE);
        assertThat(ZLcLinkType.fromType("urlLink")).isEqualTo(ZLcLinkType.URL_LINK);
    }

    @Test
    void fromTypeShouldReturnNullForUnknown() {
        assertThat(ZLcLinkType.fromType("unknown")).isNull();
    }

    @Test
    void fromTypeShouldReturnNullForNull() {
        assertThat(ZLcLinkType.fromType(null)).isNull();
    }

    @Test
    void typesShouldBeUnique() {
        ZLcLinkType[] values = ZLcLinkType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertThat(values[i].getType()).isNotEqualTo(values[j].getType());
            }
        }
    }
}