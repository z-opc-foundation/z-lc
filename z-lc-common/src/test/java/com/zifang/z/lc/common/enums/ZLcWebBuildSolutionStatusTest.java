package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebBuildSolutionStatus 单元测试
 *
 * @author zifang
 */
class ZLcWebBuildSolutionStatusTest {

    @Test
    void fromType_shouldReturnTO_BE_START_WhenToBeStart() {
        ZLcWebBuildSolutionStatus result = ZLcWebBuildSolutionStatus.fromType("toBeStart");
        assertThat(result).isEqualTo(ZLcWebBuildSolutionStatus.TO_BE_START);
    }

    @Test
    void fromType_shouldReturnIN_PROGRESS_WhenInProgress() {
        ZLcWebBuildSolutionStatus result = ZLcWebBuildSolutionStatus.fromType("inProgress");
        assertThat(result).isEqualTo(ZLcWebBuildSolutionStatus.IN_PROGRESS);
    }

    @Test
    void fromType_shouldReturnCOMPLETED_WhenCompleted() {
        ZLcWebBuildSolutionStatus result = ZLcWebBuildSolutionStatus.fromType("completed");
        assertThat(result).isEqualTo(ZLcWebBuildSolutionStatus.COMPLETED);
    }

    @Test
    void fromType_shouldReturnNull_WhenUnknown() {
        ZLcWebBuildSolutionStatus result = ZLcWebBuildSolutionStatus.fromType("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromType_shouldReturnNull_WhenNull() {
        ZLcWebBuildSolutionStatus result = ZLcWebBuildSolutionStatus.fromType(null);
        assertThat(result).isNull();
    }

    @Test
    void getType_shouldReturnCorrectValue() {
        assertThat(ZLcWebBuildSolutionStatus.TO_BE_START.getType()).isEqualTo("toBeStart");
        assertThat(ZLcWebBuildSolutionStatus.IN_PROGRESS.getType()).isEqualTo("inProgress");
        assertThat(ZLcWebBuildSolutionStatus.COMPLETED.getType()).isEqualTo("completed");
    }

    @Test
    void getDesc_shouldReturnCorrectValue() {
        assertThat(ZLcWebBuildSolutionStatus.TO_BE_START.getDesc()).isEqualTo("待开始");
        assertThat(ZLcWebBuildSolutionStatus.IN_PROGRESS.getDesc()).isEqualTo("进行中");
        assertThat(ZLcWebBuildSolutionStatus.COMPLETED.getDesc()).isEqualTo("已完成");
    }

    @Test
    void getDescByType_shouldReturnCorrectDesc() {
        assertThat(ZLcWebBuildSolutionStatus.getDescByType("toBeStart")).isEqualTo("待开始");
        assertThat(ZLcWebBuildSolutionStatus.getDescByType("completed")).isEqualTo("已完成");
    }

    @Test
    void getDescByType_shouldReturnNull_WhenInvalid() {
        assertThat(ZLcWebBuildSolutionStatus.getDescByType("invalid")).isNull();
    }

    @Test
    void isTerminal_shouldReturnTrue_WhenCompleted() {
        assertThat(ZLcWebBuildSolutionStatus.COMPLETED.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_shouldReturnFalse_WhenNotCompleted() {
        assertThat(ZLcWebBuildSolutionStatus.TO_BE_START.isTerminal()).isFalse();
        assertThat(ZLcWebBuildSolutionStatus.IN_PROGRESS.isTerminal()).isFalse();
    }
}
