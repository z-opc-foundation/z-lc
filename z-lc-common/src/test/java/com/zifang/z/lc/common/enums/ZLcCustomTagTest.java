package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCustomTag 单元测试
 */
class ZLcCustomTagTest {

    @Test
    void shouldHaveTwoValues() {
        assertThat(ZLcCustomTag.values()).hasSize(2);
    }

    @Test
    void pageTemplateShouldHaveCodeAndMessage() {
        assertThat(ZLcCustomTag.PAGE_TEMPLATE.getCode()).isEqualTo("page_template");
        assertThat(ZLcCustomTag.PAGE_TEMPLATE.getMessage()).isEqualTo("页面管理");
    }

    @Test
    void printTemplateShouldHaveCodeAndMessage() {
        assertThat(ZLcCustomTag.PRINT_TEMPLATE.getCode()).isEqualTo("print_template");
        assertThat(ZLcCustomTag.PRINT_TEMPLATE.getMessage()).isEqualTo("打印模版管理");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcCustomTag.fromCode("page_template")).isEqualTo(ZLcCustomTag.PAGE_TEMPLATE);
        assertThat(ZLcCustomTag.fromCode("print_template")).isEqualTo(ZLcCustomTag.PRINT_TEMPLATE);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcCustomTag.fromCode("unknown")).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcCustomTag.fromCode(null)).isNull();
    }

    @Test
    void codesShouldBeUnique() {
        ZLcCustomTag[] values = ZLcCustomTag.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertThat(values[i].getCode()).isNotEqualTo(values[j].getCode());
            }
        }
    }
}