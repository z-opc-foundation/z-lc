package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPrintTemplateTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcPrintTemplateTypeEnumTest {

    @Test
    void fromCode_shouldReturnFORM_When1() {
        ZLcPrintTemplateTypeEnum result = ZLcPrintTemplateTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcPrintTemplateTypeEnum.FORM);
    }

    @Test
    void fromCode_shouldReturnTABLE_When2() {
        ZLcPrintTemplateTypeEnum result = ZLcPrintTemplateTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcPrintTemplateTypeEnum.TABLE);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcPrintTemplateTypeEnum result = ZLcPrintTemplateTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcPrintTemplateTypeEnum result = ZLcPrintTemplateTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcPrintTemplateTypeEnum.FORM.getCode()).isEqualTo(1);
        assertThat(ZLcPrintTemplateTypeEnum.TABLE.getCode()).isEqualTo(2);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcPrintTemplateTypeEnum.FORM.getValue()).isEqualTo("表单");
        assertThat(ZLcPrintTemplateTypeEnum.TABLE.getValue()).isEqualTo("表格");
    }
}
