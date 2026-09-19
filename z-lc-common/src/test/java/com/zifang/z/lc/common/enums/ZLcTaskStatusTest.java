package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTaskStatus 单元测试
 */
class ZLcTaskStatusTest {

    @Test
    void shouldHaveFourValues() {
        assertThat(ZLcTaskStatus.values()).hasSize(4);
    }

    @Test
    void allShouldHaveCorrectCodes() {
        assertThat(ZLcTaskStatus.UNDO.getCode()).isEqualTo("undo");
        assertThat(ZLcTaskStatus.DOING.getCode()).isEqualTo("doing");
        assertThat(ZLcTaskStatus.DONE.getCode()).isEqualTo("done");
        assertThat(ZLcTaskStatus.ERROR.getCode()).isEqualTo("error");
    }

    @Test
    void allShouldHaveDescriptions() {
        assertThat(ZLcTaskStatus.UNDO.getDesc()).isEqualTo("未开始");
        assertThat(ZLcTaskStatus.DOING.getDesc()).isEqualTo("进行中");
        assertThat(ZLcTaskStatus.DONE.getDesc()).isEqualTo("已完成");
        assertThat(ZLcTaskStatus.ERROR.getDesc()).isEqualTo("异常节点");
    }

    @Test
    void fromCodeShouldReturnMatching() {
        assertThat(ZLcTaskStatus.fromCode("undo")).isEqualTo(ZLcTaskStatus.UNDO);
        assertThat(ZLcTaskStatus.fromCode("doing")).isEqualTo(ZLcTaskStatus.DOING);
        assertThat(ZLcTaskStatus.fromCode("done")).isEqualTo(ZLcTaskStatus.DONE);
        assertThat(ZLcTaskStatus.fromCode("error")).isEqualTo(ZLcTaskStatus.ERROR);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcTaskStatus.fromCode("unknown")).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcTaskStatus.fromCode(null)).isNull();
    }

    @Test
    void isTerminalShouldBeTrueForDoneAndError() {
        assertThat(ZLcTaskStatus.DONE.isTerminal()).isTrue();
        assertThat(ZLcTaskStatus.ERROR.isTerminal()).isTrue();
        assertThat(ZLcTaskStatus.UNDO.isTerminal()).isFalse();
        assertThat(ZLcTaskStatus.DOING.isTerminal()).isFalse();
    }

    @Test
    void isRunningShouldBeTrueForUndoAndDoing() {
        assertThat(ZLcTaskStatus.UNDO.isRunning()).isTrue();
        assertThat(ZLcTaskStatus.DOING.isRunning()).isTrue();
        assertThat(ZLcTaskStatus.DONE.isRunning()).isFalse();
        assertThat(ZLcTaskStatus.ERROR.isRunning()).isFalse();
    }

    @Test
    void codesShouldBeUnique() {
        ZLcTaskStatus[] values = ZLcTaskStatus.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertThat(values[i].getCode()).isNotEqualTo(values[j].getCode());
            }
        }
    }
}