package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataProcessingTaskStatusEnum 单元测试
 *
 * @author zifang
 */
class ZLcDataProcessingTaskStatusEnumTest {

    @Test
    void fromCode_shouldReturnUNEXECUTED_When1() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(1);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskStatusEnum.UNEXECUTED);
    }

    @Test
    void fromCode_shouldReturnIN_EXECUTION_When2() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(2);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskStatusEnum.IN_EXECUTION);
    }

    @Test
    void fromCode_shouldReturnEXECUTION_COMPLETE_When3() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(3);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskStatusEnum.EXECUTION_COMPLETE);
    }

    @Test
    void fromCode_shouldReturnEXECUTION_FAIL_When4() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(4);
        assertThat(result).isEqualTo(ZLcDataProcessingTaskStatusEnum.EXECUTION_FAIL);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcDataProcessingTaskStatusEnum result = ZLcDataProcessingTaskStatusEnum.getByCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcDataProcessingTaskStatusEnum.UNEXECUTED.getCode()).isEqualTo(1);
        assertThat(ZLcDataProcessingTaskStatusEnum.IN_EXECUTION.getCode()).isEqualTo(2);
        assertThat(ZLcDataProcessingTaskStatusEnum.EXECUTION_COMPLETE.getCode()).isEqualTo(3);
        assertThat(ZLcDataProcessingTaskStatusEnum.EXECUTION_FAIL.getCode()).isEqualTo(4);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcDataProcessingTaskStatusEnum.UNEXECUTED.getValue()).isEqualTo("待执行");
        assertThat(ZLcDataProcessingTaskStatusEnum.IN_EXECUTION.getValue()).isEqualTo("执行中");
        assertThat(ZLcDataProcessingTaskStatusEnum.EXECUTION_COMPLETE.getValue()).isEqualTo("执行完成");
        assertThat(ZLcDataProcessingTaskStatusEnum.EXECUTION_FAIL.getValue()).isEqualTo("执行失败");
    }
}
