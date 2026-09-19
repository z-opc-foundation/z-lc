package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelAuthType 单元测试
 *
 * @author zifang
 */
class ZLcLabelAuthTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelAuthType.values()).hasSize(2);
    }

    @Test
    void shouldHaveViewCode() {
        assertThat(ZLcLabelAuthType.VIEW.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveViewDescription() {
        assertThat(ZLcLabelAuthType.VIEW.getDescription()).isEqualTo("查看");
    }

    @Test
    void shouldHaveEditCode() {
        assertThat(ZLcLabelAuthType.EDIT.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveEditDescription() {
        assertThat(ZLcLabelAuthType.EDIT.getDescription()).isEqualTo("编辑");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelAuthType.fromCode(1)).isEqualTo(ZLcLabelAuthType.VIEW);
        assertThat(ZLcLabelAuthType.fromCode(0)).isEqualTo(ZLcLabelAuthType.EDIT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelAuthType.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelAuthType.fromCode(null)).isNull();
    }

    @Test
    void shouldCanEditReturnTrueForEdit() {
        assertThat(ZLcLabelAuthType.EDIT.canEdit()).isTrue();
    }

    @Test
    void shouldCanEditReturnFalseForView() {
        assertThat(ZLcLabelAuthType.VIEW.canEdit()).isFalse();
    }

    @Test
    void shouldIsViewOnlyReturnTrueForView() {
        assertThat(ZLcLabelAuthType.VIEW.isViewOnly()).isTrue();
    }

    @Test
    void shouldIsViewOnlyReturnFalseForEdit() {
        assertThat(ZLcLabelAuthType.EDIT.isViewOnly()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelAuthType.valueOf("VIEW")).isEqualTo(ZLcLabelAuthType.VIEW);
        assertThat(ZLcLabelAuthType.valueOf("EDIT")).isEqualTo(ZLcLabelAuthType.EDIT);
    }
}
