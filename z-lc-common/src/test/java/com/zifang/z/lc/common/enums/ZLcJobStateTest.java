package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcJobState 单元测试
 */
class ZLcJobStateTest {

    @Test
    void shouldHaveThreeValues() {
        assertThat(ZLcJobState.values()).hasSize(3);
    }

    @Test
    void allShouldHaveCorrectCodes() {
        assertThat(ZLcJobState.WAITING.getCode()).isEqualTo(0);
        assertThat(ZLcJobState.EXECUTING.getCode()).isEqualTo(1);
        assertThat(ZLcJobState.FINISH.getCode()).isEqualTo(2);
    }

    @Test
    void allShouldHaveDescriptions() {
        assertThat(ZLcJobState.WAITING.getDescription()).isEqualTo("等待执行");
        assertThat(ZLcJobState.EXECUTING.getDescription()).isEqualTo("执行中");
        assertThat(ZLcJobState.FINISH.getDescription()).isEqualTo("执行完成");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcJobState.fromCode(0)).isEqualTo(ZLcJobState.WAITING);
        assertThat(ZLcJobState.fromCode(1)).isEqualTo(ZLcJobState.EXECUTING);
        assertThat(ZLcJobState.fromCode(2)).isEqualTo(ZLcJobState.FINISH);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcJobState.fromCode(99)).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcJobState.fromCode(null)).isNull();
    }

    @Test
    void isTerminalShouldBeTrueOnlyForFinish() {
        assertThat(ZLcJobState.FINISH.isTerminal()).isTrue();
        assertThat(ZLcJobState.WAITING.isTerminal()).isFalse();
        assertThat(ZLcJobState.EXECUTING.isTerminal()).isFalse();
    }

    @Test
    void isRunningShouldBeTrueForWaitingAndExecuting() {
        assertThat(ZLcJobState.WAITING.isRunning()).isTrue();
        assertThat(ZLcJobState.EXECUTING.isRunning()).isTrue();
        assertThat(ZLcJobState.FINISH.isRunning()).isFalse();
    }
}