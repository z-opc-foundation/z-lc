package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebTaskStatus 单元测试
 *
 * @author zifang
 */
class ZLcWebTaskStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebTaskStatus.values()).hasSize(2);
    }

    @Test
    void shouldHaveWaitExecuteType() {
        assertThat(ZLcWebTaskStatus.WAIT_EXECUTE.getType()).isEqualTo("wait");
    }

    @Test
    void shouldHaveWaitExecuteDesc() {
        assertThat(ZLcWebTaskStatus.WAIT_EXECUTE.getDesc()).isEqualTo("待执行");
    }

    @Test
    void shouldHaveExecutedType() {
        assertThat(ZLcWebTaskStatus.EXECUTED.getType()).isEqualTo("executed");
    }

    @Test
    void shouldHaveExecutedDesc() {
        assertThat(ZLcWebTaskStatus.EXECUTED.getDesc()).isEqualTo("已执行");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebTaskStatus.fromType("wait")).isEqualTo(ZLcWebTaskStatus.WAIT_EXECUTE);
        assertThat(ZLcWebTaskStatus.fromType("executed")).isEqualTo(ZLcWebTaskStatus.EXECUTED);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebTaskStatus.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebTaskStatus.fromType(null)).isNull();
    }

    @Test
    void shouldIsExecutedReturnTrueForExecuted() {
        assertThat(ZLcWebTaskStatus.EXECUTED.isExecuted()).isTrue();
    }

    @Test
    void shouldIsExecutedReturnFalseForWaitExecute() {
        assertThat(ZLcWebTaskStatus.WAIT_EXECUTE.isExecuted()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebTaskStatus.valueOf("WAIT_EXECUTE")).isEqualTo(ZLcWebTaskStatus.WAIT_EXECUTE);
        assertThat(ZLcWebTaskStatus.valueOf("EXECUTED")).isEqualTo(ZLcWebTaskStatus.EXECUTED);
    }
}
