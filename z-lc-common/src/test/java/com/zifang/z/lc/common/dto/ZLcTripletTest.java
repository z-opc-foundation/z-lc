package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTriplet 单元测试
 */
class ZLcTripletTest {

    @Test
    void shouldCreateWithThreeValues() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);

        assertThat(triplet.getFirst()).isEqualTo("code");
        assertThat(triplet.getSecond()).isEqualTo(1);
        assertThat(triplet.getThird()).isEqualTo(100L);
    }

    @Test
    void shouldAllowNullValues() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>(null, null, null);

        assertThat(triplet.getFirst()).isNull();
        assertThat(triplet.getSecond()).isNull();
        assertThat(triplet.getThird()).isNull();
    }

    @Test
    void shouldSupportDifferentTypes() {
        ZLcTriplet<String, Boolean, Double> triplet = new ZLcTriplet<>("k", true, 3.14);

        assertThat(triplet.getFirst()).isEqualTo("k");
        assertThat(triplet.getSecond()).isTrue();
        assertThat(triplet.getThird()).isEqualTo(3.14);
    }

    @Test
    void shouldImplementSerializable() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);
        assertThat(triplet).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldExtendZLcPair() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);
        assertThat(triplet).isInstanceOf(ZLcPair.class);
    }

    @Test
    void shouldExtendZLcUnit() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);
        assertThat(triplet).isInstanceOf(ZLcUnit.class);
    }

    @Test
    void equalsShouldReturnTrueForSameValues() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>("code", 1, 100L);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentFirst() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>("other", 1, 100L);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentSecond() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>("code", 2, 100L);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentThird() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>("code", 1, 200L);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldHandleNullValues() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>(null, null, null);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>(null, null, null);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsShouldBeReflexive() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equalsShouldReturnFalseForNull() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);

        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equalsShouldReturnFalseForDifferentType() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);
        ZLcPair<String, Integer> pair = new ZLcPair<>("code", 1);

        assertThat(triplet).isNotEqualTo(pair);
    }

    @Test
    void hashCodeShouldMatchForEqualValues() {
        ZLcTriplet<String, Integer, Long> a = new ZLcTriplet<>("code", 1, 100L);
        ZLcTriplet<String, Integer, Long> b = new ZLcTriplet<>("code", 1, 100L);

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toStringShouldContainAllValues() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("code", 1, 100L);

        assertThat(triplet.toString())
            .contains("code")
            .contains("1")
            .contains("100");
    }

    @Test
    void getFirstShouldReturnFirstValue() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("alpha", 2, 3L);
        assertThat(triplet.getFirst()).isEqualTo("alpha");
    }

    @Test
    void getSecondShouldReturnSecondValue() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("alpha", 42, 3L);
        assertThat(triplet.getSecond()).isEqualTo(42);
    }

    @Test
    void getThirdShouldReturnThirdValue() {
        ZLcTriplet<String, Integer, Long> triplet = new ZLcTriplet<>("alpha", 2, 999L);
        assertThat(triplet.getThird()).isEqualTo(999L);
    }

    @Test
    void shouldSupportComplexObjectThirdValue() {
        java.util.List<Integer> list = java.util.Arrays.asList(1, 2, 3);
        ZLcTriplet<String, String, java.util.List<Integer>> triplet = new ZLcTriplet<>("k", "v", list);

        assertThat(triplet.getThird()).containsExactly(1, 2, 3);
    }
}