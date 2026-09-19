package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebBuildExecuteStatus 单元测试
 *
 * @author zifang
 */
class ZLcWebBuildExecuteStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebBuildExecuteStatus.values()).hasSize(3);
    }

    @Test
    void shouldHaveExecutingType() {
        assertThat(ZLcWebBuildExecuteStatus.EXECUTING.getType()).isEqualTo("executing");
    }

    @Test
    void shouldHaveExecutingDesc() {
        assertThat(ZLcWebBuildExecuteStatus.EXECUTING.getDesc()).isEqualTo("执行中");
    }

    @Test
    void shouldHaveSuccessType() {
        assertThat(ZLcWebBuildExecuteStatus.SUCCESS.getType()).isEqualTo("success");
    }

    @Test
    void shouldHaveSuccessDesc() {
        assertThat(ZLcWebBuildExecuteStatus.SUCCESS.getDesc()).isEqualTo("成功");
    }

    @Test
    void shouldHaveFailType() {
        assertThat(ZLcWebBuildExecuteStatus.FAIL.getType()).isEqualTo("fail");
    }

    @Test
    void shouldHaveFailDesc() {
        assertThat(ZLcWebBuildExecuteStatus.FAIL.getDesc()).isEqualTo("失败");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebBuildExecuteStatus.fromType("executing")).isEqualTo(ZLcWebBuildExecuteStatus.EXECUTING);
        assertThat(ZLcWebBuildExecuteStatus.fromType("success")).isEqualTo(ZLcWebBuildExecuteStatus.SUCCESS);
        assertThat(ZLcWebBuildExecuteStatus.fromType("fail")).isEqualTo(ZLcWebBuildExecuteStatus.FAIL);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebBuildExecuteStatus.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebBuildExecuteStatus.fromType(null)).isNull();
    }

    @Test
    void shouldIsTerminalReturnTrueForSuccess() {
        assertThat(ZLcWebBuildExecuteStatus.SUCCESS.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForFail() {
        assertThat(ZLcWebBuildExecuteStatus.FAIL.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnFalseForExecuting() {
        assertThat(ZLcWebBuildExecuteStatus.EXECUTING.isTerminal()).isFalse();
    }

    @Test
    void shouldIsSuccessReturnTrueForSuccess() {
        assertThat(ZLcWebBuildExecuteStatus.SUCCESS.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForFail() {
        assertThat(ZLcWebBuildExecuteStatus.FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldIsSuccessReturnFalseForExecuting() {
        assertThat(ZLcWebBuildExecuteStatus.EXECUTING.isSuccess()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebBuildExecuteStatus.valueOf("EXECUTING")).isEqualTo(ZLcWebBuildExecuteStatus.EXECUTING);
        assertThat(ZLcWebBuildExecuteStatus.valueOf("SUCCESS")).isEqualTo(ZLcWebBuildExecuteStatus.SUCCESS);
        assertThat(ZLcWebBuildExecuteStatus.valueOf("FAIL")).isEqualTo(ZLcWebBuildExecuteStatus.FAIL);
    }
}
