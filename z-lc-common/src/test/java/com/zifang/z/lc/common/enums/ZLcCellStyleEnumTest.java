package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCellStyleEnum 单元测试
 *
 * @author zifang
 */
class ZLcCellStyleEnumTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcCellStyleEnum.values()).hasSize(3);
    }

    @Test
    void shouldHaveLeftCode() {
        assertThat(ZLcCellStyleEnum.LEFT.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveLeftValue() {
        assertThat(ZLcCellStyleEnum.LEFT.getValue()).isEqualTo("居左");
    }

    @Test
    void shouldHaveCenterCode() {
        assertThat(ZLcCellStyleEnum.CENTER.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveCenterValue() {
        assertThat(ZLcCellStyleEnum.CENTER.getValue()).isEqualTo("居中");
    }

    @Test
    void shouldHaveRightCode() {
        assertThat(ZLcCellStyleEnum.RIGHT.getCode()).isEqualTo(3);
    }

    @Test
    void shouldHaveRightValue() {
        assertThat(ZLcCellStyleEnum.RIGHT.getValue()).isEqualTo("居右");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcCellStyleEnum.fromCode(1)).isEqualTo(ZLcCellStyleEnum.LEFT);
        assertThat(ZLcCellStyleEnum.fromCode(2)).isEqualTo(ZLcCellStyleEnum.CENTER);
        assertThat(ZLcCellStyleEnum.fromCode(3)).isEqualTo(ZLcCellStyleEnum.RIGHT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcCellStyleEnum.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcCellStyleEnum.fromCode(null)).isNull();
    }

    @Test
    void shouldGetPoiHorizontalAlignmentName() {
        assertThat(ZLcCellStyleEnum.getPoiHorizontalAlignmentName(1)).isEqualTo("LEFT");
        assertThat(ZLcCellStyleEnum.getPoiHorizontalAlignmentName(2)).isEqualTo("CENTER");
        assertThat(ZLcCellStyleEnum.getPoiHorizontalAlignmentName(3)).isEqualTo("RIGHT");
    }

    @Test
    void shouldGetPoiHorizontalAlignmentNameReturnNullForUnknown() {
        assertThat(ZLcCellStyleEnum.getPoiHorizontalAlignmentName(999)).isNull();
    }

    @Test
    void shouldGetPoiHorizontalAlignmentNameReturnNullForNull() {
        assertThat(ZLcCellStyleEnum.getPoiHorizontalAlignmentName(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcCellStyleEnum.valueOf("LEFT")).isEqualTo(ZLcCellStyleEnum.LEFT);
        assertThat(ZLcCellStyleEnum.valueOf("CENTER")).isEqualTo(ZLcCellStyleEnum.CENTER);
        assertThat(ZLcCellStyleEnum.valueOf("RIGHT")).isEqualTo(ZLcCellStyleEnum.RIGHT);
    }
}
