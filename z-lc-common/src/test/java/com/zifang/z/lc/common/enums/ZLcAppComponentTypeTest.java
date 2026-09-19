package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAppComponentType 单元测试
 *
 * @author zifang
 */
class ZLcAppComponentTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcAppComponentType.values()).hasSize(9);
    }

    @Test
    void shouldHaveModelCode() {
        assertThat(ZLcAppComponentType.MODEL.getCode()).isEqualTo("model");
    }

    @Test
    void shouldHavePageCode() {
        assertThat(ZLcAppComponentType.PAGE.getCode()).isEqualTo("page");
    }

    @Test
    void shouldHaveDictCode() {
        assertThat(ZLcAppComponentType.DICT.getCode()).isEqualTo("dict");
    }

    @Test
    void shouldHaveWorkflowCode() {
        assertThat(ZLcAppComponentType.WORKFLOW.getCode()).isEqualTo("workflow");
    }

    @Test
    void shouldHaveServiceCode() {
        assertThat(ZLcAppComponentType.SERVICE.getCode()).isEqualTo("service");
    }

    @Test
    void shouldHaveTableCode() {
        assertThat(ZLcAppComponentType.TABLE.getCode()).isEqualTo("table");
    }

    @Test
    void shouldHaveModelServiceCode() {
        assertThat(ZLcAppComponentType.MODEL_SERVICE.getCode()).isEqualTo("modelService");
    }

    @Test
    void shouldHaveCommonCode() {
        assertThat(ZLcAppComponentType.COMMON.getCode()).isEqualTo("common");
    }

    @Test
    void shouldHavePrintCode() {
        assertThat(ZLcAppComponentType.PRINT.getCode()).isEqualTo("print");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcAppComponentType.fromCode("model")).isEqualTo(ZLcAppComponentType.MODEL);
        assertThat(ZLcAppComponentType.fromCode("page")).isEqualTo(ZLcAppComponentType.PAGE);
        assertThat(ZLcAppComponentType.fromCode("dict")).isEqualTo(ZLcAppComponentType.DICT);
        assertThat(ZLcAppComponentType.fromCode("workflow")).isEqualTo(ZLcAppComponentType.WORKFLOW);
        assertThat(ZLcAppComponentType.fromCode("service")).isEqualTo(ZLcAppComponentType.SERVICE);
        assertThat(ZLcAppComponentType.fromCode("table")).isEqualTo(ZLcAppComponentType.TABLE);
        assertThat(ZLcAppComponentType.fromCode("modelService")).isEqualTo(ZLcAppComponentType.MODEL_SERVICE);
        assertThat(ZLcAppComponentType.fromCode("common")).isEqualTo(ZLcAppComponentType.COMMON);
        assertThat(ZLcAppComponentType.fromCode("print")).isEqualTo(ZLcAppComponentType.PRINT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcAppComponentType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcAppComponentType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcAppComponentType.valueOf("MODEL")).isEqualTo(ZLcAppComponentType.MODEL);
        assertThat(ZLcAppComponentType.valueOf("PAGE")).isEqualTo(ZLcAppComponentType.PAGE);
        assertThat(ZLcAppComponentType.valueOf("DICT")).isEqualTo(ZLcAppComponentType.DICT);
    }
}
