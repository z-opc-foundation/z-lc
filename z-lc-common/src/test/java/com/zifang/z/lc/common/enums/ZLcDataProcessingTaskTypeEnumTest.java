package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataProcessingTaskTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcDataProcessingTaskTypeEnumTest {

    @Test
    void fromCode_shouldReturnIMPORT_DATA_TASK_When1() {
        ZLcDataProcessingTaskTypeEnum result = ZLcDataProcessingTaskTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskTypeEnum.IMPORT_DATA_TASK);
    }

    @Test
    void fromCode_shouldReturnEXPORT_DATA_TASK_When2() {
        ZLcDataProcessingTaskTypeEnum result = ZLcDataProcessingTaskTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskTypeEnum.EXPORT_DATA_TASK);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcDataProcessingTaskTypeEnum result = ZLcDataProcessingTaskTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcDataProcessingTaskTypeEnum result = ZLcDataProcessingTaskTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcDataProcessingTaskTypeEnum.IMPORT_DATA_TASK.getCode()).isEqualTo(1);
        assertThat(ZLcDataProcessingTaskTypeEnum.EXPORT_DATA_TASK.getCode()).isEqualTo(2);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcDataProcessingTaskTypeEnum.IMPORT_DATA_TASK.getValue()).isEqualTo("数据导入任务");
        assertThat(ZLcDataProcessingTaskTypeEnum.EXPORT_DATA_TASK.getValue()).isEqualTo("数据导出任务");
    }
}
