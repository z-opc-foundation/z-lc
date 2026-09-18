package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCustomTagEnum 单元测试
 *
 * @author zifang
 */
class ZLcCustomTagEnumTest {

    @Test
    void fromCode_shouldReturnPAGE_TEMPLATE_WhenPageTemplate() {
        ZLcCustomTagEnum result = ZLcCustomTagEnum.fromCode("page_template");
        assertThat(result).isEqualTo(ZLcCustomTagEnum.PAGE_TEMPLATE);
    }

    @Test
    void fromCode_shouldReturnPRINT_TEMPLATE_WhenPrintTemplate() {
        ZLcCustomTagEnum result = ZLcCustomTagEnum.fromCode("print_template");
        assertThat(result).isEqualTo(ZLcCustomTagEnum.PRINT_TEMPLATE);
    }

    @Test
    void fromCode_shouldReturnNull_WhenUnknown() {
        ZLcCustomTagEnum result = ZLcCustomTagEnum.fromCode("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcCustomTagEnum result = ZLcCustomTagEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcCustomTagEnum.PAGE_TEMPLATE.getCode()).isEqualTo("page_template");
        assertThat(ZLcCustomTagEnum.PRINT_TEMPLATE.getCode()).isEqualTo("print_template");
    }

    @Test
    void getMessage_shouldReturnCorrectValue() {
        assertThat(ZLcCustomTagEnum.PAGE_TEMPLATE.getMessage()).isEqualTo("页面管理");
        assertThat(ZLcCustomTagEnum.PRINT_TEMPLATE.getMessage()).isEqualTo("打印模版管理");
    }
}
