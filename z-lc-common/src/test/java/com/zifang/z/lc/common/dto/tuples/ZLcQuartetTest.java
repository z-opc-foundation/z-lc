package com.zifang.z.lc.common.dto.tuples;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcQuartet 单元测试
 *
 * @author zifang
 */
class ZLcQuartetTest {

    @Test
    void shouldCreateWithConstructor() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet).isNotNull();
    }

    @Test
    void shouldGetA() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet.getA()).isEqualTo("hello");
    }

    @Test
    void shouldGetB() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet.getB()).isEqualTo(123);
    }

    @Test
    void shouldGetC() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet.getC()).isEqualTo(true);
    }

    @Test
    void shouldGetD() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet.getD()).isEqualTo(45.67);
    }

    @Test
    void shouldSetD() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        quartet.setD(99.99);
        assertThat(quartet.getD()).isEqualTo(99.99);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>(null, null, null, null);
        assertThat(quartet.getA()).isNull();
        assertThat(quartet.getB()).isNull();
        assertThat(quartet.getC()).isNull();
        assertThat(quartet.getD()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet).isInstanceOf(Serializable.class);
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet = new ZLcQuartet<>("hello", 123, true, 45.67);
        assertThat(quartet).isEqualTo(quartet);
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet1 = new ZLcQuartet<>("hello", 123, true, 45.67);
        ZLcQuartet<String, Integer, Boolean, Double> quartet2 = new ZLcQuartet<>("hello", 123, true, 99.99);
        assertThat(quartet1).isNotEqualTo(quartet2);
    }
}
