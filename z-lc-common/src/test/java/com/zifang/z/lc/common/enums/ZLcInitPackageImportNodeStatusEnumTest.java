package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcInitPackageImportNodeStatusEnum 单元测试
 *
 * @author zifang
 */
class ZLcInitPackageImportNodeStatusEnumTest {

    @Test
    void fromStatus_shouldReturnSUCCESS_WhenSuccess() {
        ZLcInitPackageImportNodeStatusEnum result = ZLcInitPackageImportNodeStatusEnum.fromStatus("success");
        assertThat(result).isEqualTo(ZLcInitPackageImportNodeStatusEnum.SUCCESS);
    }

    @Test
    void fromStatus_shouldReturnFAIL_WhenFail() {
        ZLcInitPackageImportNodeStatusEnum result = ZLcInitPackageImportNodeStatusEnum.fromStatus("fail");
        assertThat(result).isEqualTo(ZLcInitPackageImportNodeStatusEnum.FAIL);
    }

    @Test
    void fromStatus_shouldReturnEXECUTING_WhenExecuting() {
        ZLcInitPackageImportNodeStatusEnum result = ZLcInitPackageImportNodeStatusEnum.fromStatus("executing");
        assertThat(result).isEqualTo(ZLcInitPackageImportNodeStatusEnum.EXECUTING);
    }

    @Test
    void fromStatus_shouldReturnNull_WhenUnknown() {
        ZLcInitPackageImportNodeStatusEnum result = ZLcInitPackageImportNodeStatusEnum.fromStatus("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromStatus_shouldReturnNull_WhenNull() {
        ZLcInitPackageImportNodeStatusEnum result = ZLcInitPackageImportNodeStatusEnum.fromStatus(null);
        assertThat(result).isNull();
    }

    @Test
    void getStatus_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageImportNodeStatusEnum.SUCCESS.getStatus()).isEqualTo("success");
        assertThat(ZLcInitPackageImportNodeStatusEnum.FAIL.getStatus()).isEqualTo("fail");
        assertThat(ZLcInitPackageImportNodeStatusEnum.EXECUTING.getStatus()).isEqualTo("executing");
    }

    @Test
    void isTerminal_shouldReturnTrue_WhenSuccess() {
        assertThat(ZLcInitPackageImportNodeStatusEnum.SUCCESS.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_shouldReturnTrue_WhenFail() {
        assertThat(ZLcInitPackageImportNodeStatusEnum.FAIL.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_shouldReturnFalse_WhenExecuting() {
        assertThat(ZLcInitPackageImportNodeStatusEnum.EXECUTING.isTerminal()).isFalse();
    }
}
