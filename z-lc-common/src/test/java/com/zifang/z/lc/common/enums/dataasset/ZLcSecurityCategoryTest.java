package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSecurityCategory 单元测试
 *
 * @author zifang
 */
class ZLcSecurityCategoryTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcSecurityCategory.values()).hasSize(6);
    }

    @Test
    void shouldHaveC1Code() {
        assertThat(ZLcSecurityCategory.C1.getCode()).isEqualTo("C1");
    }

    @Test
    void shouldHaveC1Description() {
        assertThat(ZLcSecurityCategory.C1.getDescription()).isEqualTo("个人属性数据");
    }

    @Test
    void shouldHaveC2Code() {
        assertThat(ZLcSecurityCategory.C2.getCode()).isEqualTo("C2");
    }

    @Test
    void shouldHaveC2Description() {
        assertThat(ZLcSecurityCategory.C2.getDescription()).isEqualTo("健康状况数据");
    }

    @Test
    void shouldHaveC3Code() {
        assertThat(ZLcSecurityCategory.C3.getCode()).isEqualTo("C3");
    }

    @Test
    void shouldHaveC3Description() {
        assertThat(ZLcSecurityCategory.C3.getDescription()).isEqualTo("医疗应用数据");
    }

    @Test
    void shouldHaveC4Code() {
        assertThat(ZLcSecurityCategory.C4.getCode()).isEqualTo("C4");
    }

    @Test
    void shouldHaveC4Description() {
        assertThat(ZLcSecurityCategory.C4.getDescription()).isEqualTo("医疗支付数据");
    }

    @Test
    void shouldHaveC5Code() {
        assertThat(ZLcSecurityCategory.C5.getCode()).isEqualTo("C5");
    }

    @Test
    void shouldHaveC5Description() {
        assertThat(ZLcSecurityCategory.C5.getDescription()).isEqualTo("卫生资源数据");
    }

    @Test
    void shouldHaveC6Code() {
        assertThat(ZLcSecurityCategory.C6.getCode()).isEqualTo("C6");
    }

    @Test
    void shouldHaveC6Description() {
        assertThat(ZLcSecurityCategory.C6.getDescription()).isEqualTo("公共卫生数据");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcSecurityCategory.fromCode("C1")).isEqualTo(ZLcSecurityCategory.C1);
        assertThat(ZLcSecurityCategory.fromCode("C2")).isEqualTo(ZLcSecurityCategory.C2);
        assertThat(ZLcSecurityCategory.fromCode("C3")).isEqualTo(ZLcSecurityCategory.C3);
        assertThat(ZLcSecurityCategory.fromCode("C4")).isEqualTo(ZLcSecurityCategory.C4);
        assertThat(ZLcSecurityCategory.fromCode("C5")).isEqualTo(ZLcSecurityCategory.C5);
        assertThat(ZLcSecurityCategory.fromCode("C6")).isEqualTo(ZLcSecurityCategory.C6);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcSecurityCategory.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcSecurityCategory.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcSecurityCategory.valueOf("C1")).isEqualTo(ZLcSecurityCategory.C1);
        assertThat(ZLcSecurityCategory.valueOf("C2")).isEqualTo(ZLcSecurityCategory.C2);
        assertThat(ZLcSecurityCategory.valueOf("C3")).isEqualTo(ZLcSecurityCategory.C3);
        assertThat(ZLcSecurityCategory.valueOf("C4")).isEqualTo(ZLcSecurityCategory.C4);
        assertThat(ZLcSecurityCategory.valueOf("C5")).isEqualTo(ZLcSecurityCategory.C5);
        assertThat(ZLcSecurityCategory.valueOf("C6")).isEqualTo(ZLcSecurityCategory.C6);
    }
}
