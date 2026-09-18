package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBooleanEnum 单元测试
 *
 * @author zifang
 */
class ZLcBooleanEnumTest {

    @Test
    void getByName_shouldReturnYES_WhenYes() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByName("是");
        assertThat(result).isEqualTo(ZLcBooleanEnum.YES);
    }

    @Test
    void getByName_shouldReturnNO_WhenNo() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByName("否");
        assertThat(result).isEqualTo(ZLcBooleanEnum.NO);
    }

    @Test
    void getByName_shouldReturnNO_WhenUnknown() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByName("unknown");
        assertThat(result).isEqualTo(ZLcBooleanEnum.NO);
    }

    @Test
    void getByName_shouldReturnNO_WhenNull() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByName(null);
        assertThat(result).isEqualTo(ZLcBooleanEnum.NO);
    }

    @Test
    void getByValue_shouldReturnYES_WhenOne() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByValue(1);
        assertThat(result).isEqualTo(ZLcBooleanEnum.YES);
    }

    @Test
    void getByValue_shouldReturnNO_WhenZero() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByValue(0);
        assertThat(result).isEqualTo(ZLcBooleanEnum.NO);
    }

    @Test
    void getByValue_shouldReturnNO_WhenInvalid() {
        ZLcBooleanEnum result = ZLcBooleanEnum.getByValue(99);
        assertThat(result).isEqualTo(ZLcBooleanEnum.NO);
    }

    @Test
    void getValue_shouldReturn1_WhenYES() {
        assertThat(ZLcBooleanEnum.YES.getValue()).isEqualTo(1);
    }

    @Test
    void getValue_shouldReturn0_WhenNO() {
        assertThat(ZLcBooleanEnum.NO.getValue()).isEqualTo(0);
    }

    @Test
    void getName_shouldReturnCorrectValue() {
        assertThat(ZLcBooleanEnum.YES.getName()).isEqualTo("是");
        assertThat(ZLcBooleanEnum.NO.getName()).isEqualTo("否");
    }
}
