package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcEffectTypeStatus 单元测试
 *
 * @author zifang
 */
class ZLcEffectTypeStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcEffectTypeStatus.values()).hasSize(2);
    }

    @Test
    void shouldHaveModelCode() {
        assertThat(ZLcEffectTypeStatus.MODEL.getCode()).isEqualTo("1");
    }

    @Test
    void shouldHaveModelDescription() {
        assertThat(ZLcEffectTypeStatus.MODEL.getDescription()).isEqualTo("按模型属性");
    }

    @Test
    void shouldHaveSecurityCategoryAndLevelCode() {
        assertThat(ZLcEffectTypeStatus.SECURITY_CATEGORY_AND_LEVEL.getCode()).isEqualTo("2");
    }

    @Test
    void shouldHaveSecurityCategoryAndLevelDescription() {
        assertThat(ZLcEffectTypeStatus.SECURITY_CATEGORY_AND_LEVEL.getDescription()).isEqualTo("按分级分类");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcEffectTypeStatus.fromCode("1")).isEqualTo(ZLcEffectTypeStatus.MODEL);
        assertThat(ZLcEffectTypeStatus.fromCode("2")).isEqualTo(ZLcEffectTypeStatus.SECURITY_CATEGORY_AND_LEVEL);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcEffectTypeStatus.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcEffectTypeStatus.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcEffectTypeStatus.valueOf("MODEL")).isEqualTo(ZLcEffectTypeStatus.MODEL);
        assertThat(ZLcEffectTypeStatus.valueOf("SECURITY_CATEGORY_AND_LEVEL")).isEqualTo(ZLcEffectTypeStatus.SECURITY_CATEGORY_AND_LEVEL);
    }
}
