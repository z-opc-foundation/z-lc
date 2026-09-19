package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPageTemplateOperateRecordType 单元测试
 *
 * @author zifang
 */
class ZLcPageTemplateOperateRecordTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcPageTemplateOperateRecordType.values()).hasSize(1);
    }

    @Test
    void shouldHavePrintType() {
        assertThat(ZLcPageTemplateOperateRecordType.PRINT.getType()).isEqualTo("print");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcPageTemplateOperateRecordType.fromType("print")).isEqualTo(ZLcPageTemplateOperateRecordType.PRINT);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcPageTemplateOperateRecordType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcPageTemplateOperateRecordType.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcPageTemplateOperateRecordType.valueOf("PRINT")).isEqualTo(ZLcPageTemplateOperateRecordType.PRINT);
    }
}
