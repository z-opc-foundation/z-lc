package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPageTemplateType 单元测试
 *
 * @author zifang
 */
class ZLcPageTemplateTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcPageTemplateType.values()).hasSize(7);
    }

    @Test
    void shouldHaveDefaultPageTypes() {
        assertThat(ZLcPageTemplateType.DEFAULT_PAGE_TYPES).hasSize(7);
    }

    @Test
    void shouldHaveListPageType() {
        assertThat(ZLcPageTemplateType.LIST.getPageType()).isEqualTo("list");
        assertThat(ZLcPageTemplateType.LIST.getPageTypeDesc()).isEqualTo("列表页");
    }

    @Test
    void shouldHaveFormPageType() {
        assertThat(ZLcPageTemplateType.FORM.getPageType()).isEqualTo("form");
        assertThat(ZLcPageTemplateType.FORM.getPageTypeDesc()).isEqualTo("表单页");
    }

    @Test
    void shouldHaveAppFormPageType() {
        assertThat(ZLcPageTemplateType.APP_FORM.getPageType()).isEqualTo("app_form");
        assertThat(ZLcPageTemplateType.APP_FORM.getPageTypeDesc()).isEqualTo("移动端表单页");
    }

    @Test
    void shouldHaveFormV2PageType() {
        assertThat(ZLcPageTemplateType.FORM_V2.getPageType()).isEqualTo("formV2");
        assertThat(ZLcPageTemplateType.FORM_V2.getPageTypeDesc()).isEqualTo("表单页-新版编辑器");
    }

    @Test
    void shouldHaveListV2PageType() {
        assertThat(ZLcPageTemplateType.LIST_V2.getPageType()).isEqualTo("listV2");
        assertThat(ZLcPageTemplateType.LIST_V2.getPageTypeDesc()).isEqualTo("列表页-新版编辑器");
    }

    @Test
    void shouldHaveCommonPageType() {
        assertThat(ZLcPageTemplateType.COMMON.getPageType()).isEqualTo("common");
        assertThat(ZLcPageTemplateType.COMMON.getPageTypeDesc()).isEqualTo("普通页");
    }

    @Test
    void shouldHavePrintPageType() {
        assertThat(ZLcPageTemplateType.PRINT.getPageType()).isEqualTo("print");
        assertThat(ZLcPageTemplateType.PRINT.getPageTypeDesc()).isEqualTo("打印模板页");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcPageTemplateType.fromCode("list")).isEqualTo(ZLcPageTemplateType.LIST);
        assertThat(ZLcPageTemplateType.fromCode("form")).isEqualTo(ZLcPageTemplateType.FORM);
        assertThat(ZLcPageTemplateType.fromCode("app_form")).isEqualTo(ZLcPageTemplateType.APP_FORM);
        assertThat(ZLcPageTemplateType.fromCode("formV2")).isEqualTo(ZLcPageTemplateType.FORM_V2);
        assertThat(ZLcPageTemplateType.fromCode("listV2")).isEqualTo(ZLcPageTemplateType.LIST_V2);
        assertThat(ZLcPageTemplateType.fromCode("common")).isEqualTo(ZLcPageTemplateType.COMMON);
        assertThat(ZLcPageTemplateType.fromCode("print")).isEqualTo(ZLcPageTemplateType.PRINT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcPageTemplateType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcPageTemplateType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcPageTemplateType.valueOf("LIST")).isEqualTo(ZLcPageTemplateType.LIST);
        assertThat(ZLcPageTemplateType.valueOf("FORM")).isEqualTo(ZLcPageTemplateType.FORM);
        assertThat(ZLcPageTemplateType.valueOf("APP_FORM")).isEqualTo(ZLcPageTemplateType.APP_FORM);
    }
}
