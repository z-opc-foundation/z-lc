package com.zifang.z.lc.common.dto.tuples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPair (tuples包版本) 单元测试
 *
 * @author zifang
 */
class ZLcPairTest {

    @Test
    void shouldCreatePairWithValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("age", 25);
        assertThat(pair.getA()).isEqualTo("age");
        assertThat(pair.getB()).isEqualTo(25);
    }

    @Test
    void shouldSetAndGetB() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("age", 25);
        pair.setB(30);
        assertThat(pair.getB()).isEqualTo(30);
    }

    @Test
    void shouldImplementSerializable() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("age", 25);
        assertThat(pair).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldReturnCorrectToString() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("age", 25);
        assertThat(pair.toString()).isEqualTo("age:25");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>(null, null);
        assertThat(pair.getA()).isNull();
        assertThat(pair.getB()).isNull();
    }

    @Test
    void shouldInheritUnitBehavior() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("age", 25);
        pair.setA("weight");
        assertThat(pair.getA()).isEqualTo("weight");
    }
}