package com.zifang.z.lc.common.dto.tuples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcUnit (tuples包版本) 单元测试
 *
 * @author zifang
 */
class ZLcUnitTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcUnit<String> unit = new ZLcUnit<>();
        assertThat(unit).isNotNull();
        assertThat(unit.getA()).isNull();
    }

    @Test
    void shouldCreateWithValue() {
        ZLcUnit<String> unit = new ZLcUnit<>("test");
        assertThat(unit.getA()).isEqualTo("test");
    }

    @Test
    void shouldSetAndGetA() {
        ZLcUnit<String> unit = new ZLcUnit<>();
        unit.setA("modified");
        assertThat(unit.getA()).isEqualTo("modified");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcUnit<String> unit = new ZLcUnit<>("test");
        assertThat(unit).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldReturnCorrectToString() {
        ZLcUnit<String> unit = new ZLcUnit<>("test");
        assertThat(unit.toString()).isEqualTo("test");
    }

    @Test
    void shouldHandleNullValueInToString() {
        ZLcUnit<String> unit = new ZLcUnit<>();
        assertThat(unit.toString()).isEqualTo("null");
    }
}