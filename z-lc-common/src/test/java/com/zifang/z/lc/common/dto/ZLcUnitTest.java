package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcUnit 单元测试
 */
class ZLcUnitTest {

    @Test
    void shouldCreateWithValue() {
        ZLcUnit<String> unit = new ZLcUnit<>("hello");

        assertThat(unit.getFirst()).isEqualTo("hello");
    }

    @Test
    void shouldAllowNullValue() {
        ZLcUnit<String> unit = new ZLcUnit<>(null);

        assertThat(unit.getFirst()).isNull();
    }

    @Test
    void shouldSupportDifferentTypes() {
        ZLcUnit<Integer> intUnit = new ZLcUnit<>(42);
        ZLcUnit<Long> longUnit = new ZLcUnit<>(100L);
        ZLcUnit<Boolean> boolUnit = new ZLcUnit<>(true);

        assertThat(intUnit.getFirst()).isEqualTo(42);
        assertThat(longUnit.getFirst()).isEqualTo(100L);
        assertThat(boolUnit.getFirst()).isTrue();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcUnit<String> unit = new ZLcUnit<>("test");
        assertThat(unit).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void equalsShouldReturnTrueForSameValue() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");
        ZLcUnit<String> b = new ZLcUnit<>("hello");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsShouldReturnFalseForDifferentValue() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");
        ZLcUnit<String> b = new ZLcUnit<>("world");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsShouldBeReflexive() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equalsShouldReturnFalseForNull() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");

        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equalsShouldReturnFalseForDifferentType() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");

        assertThat(a).isNotEqualTo("hello");
    }

    @Test
    void hashCodeShouldBeConsistent() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");

        assertThat(a.hashCode()).isEqualTo(a.hashCode());
    }

    @Test
    void hashCodeShouldMatchForEqualValues() {
        ZLcUnit<String> a = new ZLcUnit<>("hello");
        ZLcUnit<String> b = new ZLcUnit<>("hello");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toStringShouldContainValue() {
        ZLcUnit<String> unit = new ZLcUnit<>("hello");

        assertThat(unit.toString()).contains("hello");
    }

    @Test
    void shouldSupportComplexObjects() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        ZLcUnit<java.util.List<String>> unit = new ZLcUnit<>(list);

        assertThat(unit.getFirst()).containsExactly("a", "b", "c");
    }
}