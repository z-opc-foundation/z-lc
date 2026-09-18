package com.zifang.z.lc.common.dto.tuples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 元组类 ZLcUnit / ZLcPair / ZLcTriplet / ZLcQuartet 单元测试
 *
 * @author zifang
 */
class ZLcTupleTest {

    // ========== ZLcUnit ==========

    @Test
    void unit_shouldCreateWithDefaultConstructor() {
        ZLcUnit<String> unit = new ZLcUnit<>();
        assertThat(unit.getA()).isNull();
    }

    @Test
    void unit_shouldCreateWithValue() {
        ZLcUnit<String> unit = new ZLcUnit<>("hello");
        assertThat(unit.getA()).isEqualTo("hello");
    }

    @Test
    void unit_shouldSetAndGet() {
        ZLcUnit<Integer> unit = new ZLcUnit<>();
        unit.setA(42);
        assertThat(unit.getA()).isEqualTo(42);
    }

    @Test
    void unit_toString_shouldReturnElementValue() {
        ZLcUnit<String> unit = new ZLcUnit<>("test");
        assertThat(unit.toString()).isEqualTo("test");
    }

    @Test
    void unit_toString_shouldReturnNull_WhenNullValue() {
        ZLcUnit<String> unit = new ZLcUnit<>(null);
        assertThat(unit.toString()).isEqualTo("null");
    }

    // ========== ZLcPair ==========

    @Test
    void pair_shouldCreateWithTwoValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("key", 100);
        assertThat(pair.getA()).isEqualTo("key");
        assertThat(pair.getB()).isEqualTo(100);
    }

    @Test
    void pair_toString_shouldReturnColonSeparated() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("a", 1);
        assertThat(pair.toString()).isEqualTo("a:1");
    }

    @Test
    void pair_shouldSetB() {
        ZLcPair<String, String> pair = new ZLcPair<>("x", "old");
        pair.setB("new");
        assertThat(pair.getB()).isEqualTo("new");
    }

    // ========== ZLcTriplet ==========

    @Test
    void triplet_shouldCreateWithThreeValues() {
        ZLcTriplet<String, Integer, Boolean> triplet =
                new ZLcTriplet<>("name", 42, true);
        assertThat(triplet.getA()).isEqualTo("name");
        assertThat(triplet.getB()).isEqualTo(42);
        assertThat(triplet.getC()).isTrue();
    }

    @Test
    void triplet_shouldSetC() {
        ZLcTriplet<String, Integer, Boolean> triplet =
                new ZLcTriplet<>("x", 1, false);
        triplet.setC(true);
        assertThat(triplet.getC()).isTrue();
    }

    // ========== ZLcQuartet ==========

    @Test
    void quartet_shouldCreateWithFourValues() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet =
                new ZLcQuartet<>("a", 1, true, 3.14);
        assertThat(quartet.getA()).isEqualTo("a");
        assertThat(quartet.getB()).isEqualTo(1);
        assertThat(quartet.getC()).isTrue();
        assertThat(quartet.getD()).isEqualTo(3.14);
    }

    @Test
    void quartet_shouldSetD() {
        ZLcQuartet<String, Integer, Boolean, Double> quartet =
                new ZLcQuartet<>("x", 1, false, 0.0);
        quartet.setD(9.99);
        assertThat(quartet.getD()).isEqualTo(9.99);
    }

    // ========== 继承链 ==========

    @Test
    void quartet_shouldInheritPairMethods() {
        ZLcQuartet<String, String, String, String> q =
                new ZLcQuartet<>("a", "b", "c", "d");
        // 可以通过 Pair 的 getB 访问
        assertThat(q.getB()).isEqualTo("b");
        assertThat(q.getC()).isEqualTo("c");
    }
}
