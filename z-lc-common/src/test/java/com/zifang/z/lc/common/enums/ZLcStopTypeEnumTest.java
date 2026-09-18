package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStopTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcStopTypeEnumTest {

    @Test
    void fromCode_shouldReturnSTOP_DELETE_When1() {
        ZLcStopTypeEnum result = ZLcStopTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcStopTypeEnum.STOP_DELETE);
    }

    @Test
    void fromCode_shouldReturnSTOP_SUSPEND_When2() {
        ZLcStopTypeEnum result = ZLcStopTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcStopTypeEnum.STOP_SUSPEND);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcStopTypeEnum result = ZLcStopTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcStopTypeEnum result = ZLcStopTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getStopType_shouldReturn1_WhenSTOP_DELETE() {
        assertThat(ZLcStopTypeEnum.STOP_DELETE.getStopType()).isEqualTo(1);
    }

    @Test
    void getStopType_shouldReturn2_WhenSTOP_SUSPEND() {
        assertThat(ZLcStopTypeEnum.STOP_SUSPEND.getStopType()).isEqualTo(2);
    }

    @Test
    void getStopTypeName_shouldReturnCorrectValue() {
        assertThat(ZLcStopTypeEnum.STOP_DELETE.getStopTypeName()).isEqualTo("删除");
        assertThat(ZLcStopTypeEnum.STOP_SUSPEND.getStopTypeName()).isEqualTo("挂起");
    }
}
