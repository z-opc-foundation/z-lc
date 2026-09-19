package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFetchValueFillStrategy 单元测试
 *
 * @author zifang
 */
class ZLcFetchValueFillStrategyTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcFetchValueFillStrategy.values()).hasSize(4);
    }

    @Test
    void shouldHaveDefaultStrategies() {
        assertThat(ZLcFetchValueFillStrategy.DEFAULT_STRATEGIES).hasSize(4);
    }

    @Test
    void shouldHaveStaticValue() {
        assertThat(ZLcFetchValueFillStrategy.STATIC_VALUE.getName()).isEqualTo("固定值");
        assertThat(ZLcFetchValueFillStrategy.STATIC_VALUE.getStrategyType()).isEqualTo("STATIC_VALUE");
    }

    @Test
    void shouldHaveContextValue() {
        assertThat(ZLcFetchValueFillStrategy.CONTEXT_VALUE.getName()).isEqualTo("上下文取值");
        assertThat(ZLcFetchValueFillStrategy.CONTEXT_VALUE.getStrategyType()).isEqualTo("CONTEXT_VALUE");
    }

    @Test
    void shouldHaveDictValue() {
        assertThat(ZLcFetchValueFillStrategy.DICT_VALUE.getName()).isEqualTo("字典取值");
        assertThat(ZLcFetchValueFillStrategy.DICT_VALUE.getStrategyType()).isEqualTo("DICT_VALUE");
    }

    @Test
    void shouldHaveServiceValue() {
        assertThat(ZLcFetchValueFillStrategy.SERVICE_VALUE.getName()).isEqualTo("服务取值");
        assertThat(ZLcFetchValueFillStrategy.SERVICE_VALUE.getStrategyType()).isEqualTo("SERVICE_VALUE");
    }

    @Test
    void shouldFromStrategyType() {
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType("STATIC_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.STATIC_VALUE);
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType("CONTEXT_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.CONTEXT_VALUE);
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType("DICT_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.DICT_VALUE);
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType("SERVICE_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.SERVICE_VALUE);
    }

    @Test
    void shouldReturnNullForUnknownFromStrategyType() {
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromStrategyType() {
        assertThat(ZLcFetchValueFillStrategy.fromStrategyType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcFetchValueFillStrategy.valueOf("STATIC_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.STATIC_VALUE);
        assertThat(ZLcFetchValueFillStrategy.valueOf("CONTEXT_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.CONTEXT_VALUE);
        assertThat(ZLcFetchValueFillStrategy.valueOf("DICT_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.DICT_VALUE);
        assertThat(ZLcFetchValueFillStrategy.valueOf("SERVICE_VALUE")).isEqualTo(ZLcFetchValueFillStrategy.SERVICE_VALUE);
    }
}
