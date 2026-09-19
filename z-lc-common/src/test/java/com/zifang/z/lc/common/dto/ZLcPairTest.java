package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPair 单元测试
 */
class ZLcPairTest {

    @Test
    void shouldCreateWithTwoValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("key", 42);

        assertThat(pair.getFirst()).isEqualTo("key");
        assertThat(pair.getSecond()).isEqualTo(42);
    }

    @Test
    void shouldAllowNullValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>(null, null);

        assertThat(pair.getFirst()).isNull();
        assertThat(pair.getSecond()).isNull();
    }

    @Test
    void shouldSupportDifferentTypes() {
        ZLcPair<String, String> sp = new ZLcPair<>("a", "b");
        ZLcPair<Integer, Long> ip = new ZLcPair<>(1, 2L);
        ZLcPair<Boolean, Double> bp = new ZLcPair<>(true, 3.14);

        assertThat(sp.getFirst()).isEqualTo("a");
        assertThat(ip.getSecond()).isEqualTo(2L);
        assertThat(bp.getFirst()).isTrue();
        assertThat(bp.getSecond()).isEqualTo(3.14);
    }

    @Test
    void shouldImplementSerializable() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("key", 1);
        assertThat(pair).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldExtendZLcUnit() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("key", 1);
        assertThat(pair).isInstanceOf(ZLcUnit.class);
    }

    @Test
    void equalsShouldReturnTrueForSameValues() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);
        ZLcPair<String, Integer> b = new ZLcPair<>("key", 42);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentFirst() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);
        ZLcPair<String, Integer> b = new ZLcPair<>("other", 42);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentSecond() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);
        ZLcPair<String, Integer> b = new ZLcPair<>("key", 100);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldHandleNullValues() {
        ZLcPair<String, Integer> a = new ZLcPair<>(null, null);
        ZLcPair<String, Integer> b = new ZLcPair<>(null, null);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsShouldBeReflexive() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equalsShouldReturnFalseForNull() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);

        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equalsShouldReturnFalseForDifferentType() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);

        assertThat(a).isNotEqualTo("string");
    }

    @Test
    void equalsShouldBeConsistentWithHashCode() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);
        ZLcPair<String, Integer> b = new ZLcPair<>("key", 42);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void hashCodeShouldMatchForEqualValues() {
        ZLcPair<String, Integer> a = new ZLcPair<>("key", 42);
        ZLcPair<String, Integer> b = new ZLcPair<>("key", 42);

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toStringShouldContainBothValues() {
        ZLcPair<String, Integer> pair = new ZLcPair<>("key", 42);

        assertThat(pair.toString()).contains("key").contains("42");
    }
}