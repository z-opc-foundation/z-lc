package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FormOperationMode 单元测试
 *
 * @author zifang
 */
class FormOperationModeTest {

    @Test
    void shouldHaveCorrectModeForTEMP() {
        assertThat(FormOperationMode.TEMP.getMode()).isEqualTo(1);
    }

    @Test
    void shouldHaveCorrectModeNameForTEMP() {
        assertThat(FormOperationMode.TEMP.getModeName()).isEqualTo("暂存");
    }

    @Test
    void shouldHaveCorrectModeForSUBMIT() {
        assertThat(FormOperationMode.SUBMIT.getMode()).isEqualTo(2);
    }

    @Test
    void shouldHaveCorrectModeNameForSUBMIT() {
        assertThat(FormOperationMode.SUBMIT.getModeName()).isEqualTo("提交");
    }

    @Test
    void shouldHaveCorrectModeForAUDIT() {
        assertThat(FormOperationMode.AUDIT.getMode()).isEqualTo(3);
    }

    @Test
    void shouldHaveCorrectModeNameForAUDIT() {
        assertThat(FormOperationMode.AUDIT.getModeName()).isEqualTo("审批修改保存");
    }

    @Test
    void shouldReturnEnumByModeForTEMP() {
        assertThat(FormOperationMode.fromMode(1)).isEqualTo(FormOperationMode.TEMP);
    }

    @Test
    void shouldReturnEnumByModeForSUBMIT() {
        assertThat(FormOperationMode.fromMode(2)).isEqualTo(FormOperationMode.SUBMIT);
    }

    @Test
    void shouldReturnEnumByModeForAUDIT() {
        assertThat(FormOperationMode.fromMode(3)).isEqualTo(FormOperationMode.AUDIT);
    }

    @Test
    void shouldReturnNullForUnknownMode() {
        assertThat(FormOperationMode.fromMode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullMode() {
        assertThat(FormOperationMode.fromMode(null)).isNull();
    }

    @Test
    void shouldHaveThreeValues() {
        assertThat(FormOperationMode.values()).hasSize(3);
    }
}