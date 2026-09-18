package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcHomeExportTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcHomeExportTypeEnumTest {

    @Test
    void fromCode_shouldReturnTODO_TASK_When1() {
        ZLcHomeExportTypeEnum result = ZLcHomeExportTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcHomeExportTypeEnum.TODO_TASK);
    }

    @Test
    void fromCode_shouldReturnDONE_TASK_When2() {
        ZLcHomeExportTypeEnum result = ZLcHomeExportTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcHomeExportTypeEnum.DONE_TASK);
    }

    @Test
    void fromCode_shouldReturnDONE_INITIATE_When3() {
        ZLcHomeExportTypeEnum result = ZLcHomeExportTypeEnum.fromCode(3);
        assertThat(result).isEqualTo(ZLcHomeExportTypeEnum.DONE_INITIATE);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcHomeExportTypeEnum result = ZLcHomeExportTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcHomeExportTypeEnum result = ZLcHomeExportTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcHomeExportTypeEnum.TODO_TASK.getCode()).isEqualTo(1);
        assertThat(ZLcHomeExportTypeEnum.DONE_TASK.getCode()).isEqualTo(2);
        assertThat(ZLcHomeExportTypeEnum.DONE_INITIATE.getCode()).isEqualTo(3);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcHomeExportTypeEnum.TODO_TASK.getValue()).isEqualTo("待办");
        assertThat(ZLcHomeExportTypeEnum.DONE_TASK.getValue()).isEqualTo("已办");
        assertThat(ZLcHomeExportTypeEnum.DONE_INITIATE.getValue()).isEqualTo("已发");
    }
}
