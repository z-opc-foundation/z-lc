package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPackageExecute 单元测试
 */
class ZLcPackageExecuteTest {

    @Test
    void shouldHaveFourValues() {
        assertThat(ZLcPackageExecute.values()).hasSize(4);
    }

    @Test
    void allShouldHaveCorrectCodes() {
        assertThat(ZLcPackageExecute.UN_EXECUTE.getCode()).isEqualTo(0);
        assertThat(ZLcPackageExecute.EXECUTING.getCode()).isEqualTo(1);
        assertThat(ZLcPackageExecute.EXECUTED.getCode()).isEqualTo(2);
        assertThat(ZLcPackageExecute.FAIL.getCode()).isEqualTo(3);
    }

    @Test
    void allShouldHaveNames() {
        assertThat(ZLcPackageExecute.UN_EXECUTE.getName()).isEqualTo("待执行");
        assertThat(ZLcPackageExecute.EXECUTING.getName()).isEqualTo("执行中");
        assertThat(ZLcPackageExecute.EXECUTED.getName()).isEqualTo("执行成功");
        assertThat(ZLcPackageExecute.FAIL.getName()).isEqualTo("执行失败");
    }

    @Test
    void findByCodeShouldReturnMatching() {
        assertThat(ZLcPackageExecute.findByCode(0)).isEqualTo(ZLcPackageExecute.UN_EXECUTE);
        assertThat(ZLcPackageExecute.findByCode(1)).isEqualTo(ZLcPackageExecute.EXECUTING);
        assertThat(ZLcPackageExecute.findByCode(2)).isEqualTo(ZLcPackageExecute.EXECUTED);
        assertThat(ZLcPackageExecute.findByCode(3)).isEqualTo(ZLcPackageExecute.FAIL);
    }

    @Test
    void findByCodeShouldReturnNullForUnknown() {
        assertThat(ZLcPackageExecute.findByCode(99)).isNull();
    }

    @Test
    void findByCodeShouldReturnNullForNull() {
        assertThat(ZLcPackageExecute.findByCode(null)).isNull();
    }

    @Test
    void isTerminalShouldBeTrueForExecutedAndFail() {
        assertThat(ZLcPackageExecute.EXECUTED.isTerminal()).isTrue();
        assertThat(ZLcPackageExecute.FAIL.isTerminal()).isTrue();
    }

    @Test
    void isTerminalShouldBeFalseForNonTerminal() {
        assertThat(ZLcPackageExecute.UN_EXECUTE.isTerminal()).isFalse();
        assertThat(ZLcPackageExecute.EXECUTING.isTerminal()).isFalse();
    }

    @Test
    void isSuccessShouldBeTrueOnlyForExecuted() {
        assertThat(ZLcPackageExecute.EXECUTED.isSuccess()).isTrue();
        assertThat(ZLcPackageExecute.UN_EXECUTE.isSuccess()).isFalse();
        assertThat(ZLcPackageExecute.EXECUTING.isSuccess()).isFalse();
        assertThat(ZLcPackageExecute.FAIL.isSuccess()).isFalse();
    }

    @Test
    void isFailShouldBeTrueOnlyForFail() {
        assertThat(ZLcPackageExecute.FAIL.isFail()).isTrue();
        assertThat(ZLcPackageExecute.UN_EXECUTE.isFail()).isFalse();
        assertThat(ZLcPackageExecute.EXECUTING.isFail()).isFalse();
        assertThat(ZLcPackageExecute.EXECUTED.isFail()).isFalse();
    }

    @Test
    void codesShouldBeUnique() {
        ZLcPackageExecute[] values = ZLcPackageExecute.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertThat(values[i].getCode()).isNotEqualTo(values[j].getCode());
            }
        }
    }
}