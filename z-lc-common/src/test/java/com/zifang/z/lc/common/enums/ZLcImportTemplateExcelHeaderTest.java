package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcImportTemplateExcelHeader 单元测试
 *
 * @author zifang
 */
class ZLcImportTemplateExcelHeaderTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcImportTemplateExcelHeader.values()).hasSize(1);
    }

    @Test
    void shouldHaveLabelValueHeaderList() {
        assertThat(ZLcImportTemplateExcelHeader.LABEL_VALUE.getHeaderList()).hasSize(2);
        assertThat(ZLcImportTemplateExcelHeader.LABEL_VALUE.getHeaderList()).containsExactly("列名", "标签值");
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcImportTemplateExcelHeader.valueOf("LABEL_VALUE")).isEqualTo(ZLcImportTemplateExcelHeader.LABEL_VALUE);
    }
}
