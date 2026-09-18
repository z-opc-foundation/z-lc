package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataImportModeEnum 单元测试
 *
 * @author zifang
 */
class ZLcDataImportModeEnumTest {

    @Test
    void fromCode_shouldReturnFULL_UPDATE_When1() {
        ZLcDataImportModeEnum result = ZLcDataImportModeEnum.getByCode(1);
        assertThat(result).isEqualTo(ZLcDataImportModeEnum.FULL_UPDATE);
    }

    @Test
    void fromCode_shouldReturnINCREMENTAL_UPDATE_When2() {
        ZLcDataImportModeEnum result = ZLcDataImportModeEnum.getByCode(2);
        assertThat(result).isEqualTo(ZLcDataImportModeEnum.INCREMENTAL_UPDATE);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcDataImportModeEnum result = ZLcDataImportModeEnum.getByCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcDataImportModeEnum result = ZLcDataImportModeEnum.getByCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturn1_WhenFULL_UPDATE() {
        assertThat(ZLcDataImportModeEnum.FULL_UPDATE.getCode()).isEqualTo(1);
    }

    @Test
    void getCode_shouldReturn2_WhenINCREMENTAL_UPDATE() {
        assertThat(ZLcDataImportModeEnum.INCREMENTAL_UPDATE.getCode()).isEqualTo(2);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcDataImportModeEnum.FULL_UPDATE.getValue()).isEqualTo("全量更新");
        assertThat(ZLcDataImportModeEnum.INCREMENTAL_UPDATE.getValue()).isEqualTo("增量更新");
    }
}
