package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPackageExecuteStatusEnum 单元测试
 *
 * @author zifang
 */
class ZLcPackageExecuteStatusEnumTest {

    @Test
    void fromCode_shouldReturnINIT_WhenInit() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode("init");
        assertThat(result).isEqualTo(ZLcPackageExecuteStatusEnum.INIT);
    }

    @Test
    void fromCode_shouldReturnEXECUTING_WhenExecuting() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode("executing");
        assertThat(result).isEqualTo(ZLcPackageExecuteStatusEnum.EXECUTING);
    }

    @Test
    void fromCode_shouldReturnSUCCESS_WhenSuccess() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode("success");
        assertThat(result).isEqualTo(ZLcPackageExecuteStatusEnum.SUCCESS);
    }

    @Test
    void fromCode_shouldReturnFAIL_WhenFail() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode("fail");
        assertThat(result).isEqualTo(ZLcPackageExecuteStatusEnum.FAIL);
    }

    @Test
    void fromCode_shouldReturnNull_WhenUnknown() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcPackageExecuteStatusEnum result = ZLcPackageExecuteStatusEnum.getByCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcPackageExecuteStatusEnum.INIT.getCode()).isEqualTo("init");
        assertThat(ZLcPackageExecuteStatusEnum.EXECUTING.getCode()).isEqualTo("executing");
        assertThat(ZLcPackageExecuteStatusEnum.SUCCESS.getCode()).isEqualTo("success");
        assertThat(ZLcPackageExecuteStatusEnum.FAIL.getCode()).isEqualTo("fail");
    }

    @Test
    void getDesc_shouldReturnCorrectValue() {
        assertThat(ZLcPackageExecuteStatusEnum.INIT.getDesc()).isEqualTo("初始化");
        assertThat(ZLcPackageExecuteStatusEnum.EXECUTING.getDesc()).isEqualTo("执行中");
        assertThat(ZLcPackageExecuteStatusEnum.SUCCESS.getDesc()).isEqualTo("执行成功");
        assertThat(ZLcPackageExecuteStatusEnum.FAIL.getDesc()).isEqualTo("执行失败");
    }

    @Test
    void getDescByCode_shouldReturnCorrectDesc() {
        assertThat(ZLcPackageExecuteStatusEnum.INIT.getDescByCode("init")).isEqualTo("初始化");
        assertThat(ZLcPackageExecuteStatusEnum.FAIL.getDescByCode("fail")).isEqualTo("执行失败");
    }

    @Test
    void getDescByCode_shouldReturnNull_WhenInvalid() {
        assertThat(ZLcPackageExecuteStatusEnum.INIT.getDescByCode("invalid")).isNull();
    }
}
