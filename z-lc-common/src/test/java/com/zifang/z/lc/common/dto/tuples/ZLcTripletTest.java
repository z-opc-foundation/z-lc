package com.zifang.z.lc.common.dto.tuples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTriplet (tuples包版本) 单元测试
 *
 * @author zifang
 */
class ZLcTripletTest {

    @Test
    void shouldCreateTripletWithValues() {
        ZLcTriplet<String, String, Integer> triplet = new ZLcTriplet<>("user", "age", 25);
        assertThat(triplet.getA()).isEqualTo("user");
        assertThat(triplet.getB()).isEqualTo("age");
        assertThat(triplet.getC()).isEqualTo(25);
    }

    @Test
    void shouldSetAndGetC() {
        ZLcTriplet<String, String, Integer> triplet = new ZLcTriplet<>("user", "age", 25);
        triplet.setC(30);
        assertThat(triplet.getC()).isEqualTo(30);
    }

    @Test
    void shouldImplementSerializable() {
        ZLcTriplet<String, String, Integer> triplet = new ZLcTriplet<>("user", "age", 25);
        assertThat(triplet).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcTriplet<String, String, Integer> triplet = new ZLcTriplet<>(null, null, null);
        assertThat(triplet.getA()).isNull();
        assertThat(triplet.getB()).isNull();
        assertThat(triplet.getC()).isNull();
    }

    @Test
    void shouldInheritPairBehavior() {
        ZLcTriplet<String, String, Integer> triplet = new ZLcTriplet<>("user", "age", 25);
        triplet.setA("admin");
        triplet.setB("name");
        triplet.setC(100);

        assertThat(triplet.getA()).isEqualTo("admin");
        assertThat(triplet.getB()).isEqualTo("name");
        assertThat(triplet.getC()).isEqualTo(100);
    }
}