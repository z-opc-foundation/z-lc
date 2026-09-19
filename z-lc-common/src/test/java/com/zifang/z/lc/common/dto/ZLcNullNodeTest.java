package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcNullNode 单元测试
 */
class ZLcNullNodeTest {

    @Test
    void shouldHaveSingleInstance() {
        assertThat(ZLcNullNode.INSTANCE).isNotNull();
        assertThat(ZLcNullNode.INSTANCE).isSameAs(ZLcNullNode.INSTANCE);
    }

    @Test
    void isNullNodeShouldReturnTrueForInstance() {
        assertThat(ZLcNullNode.isNullNode(ZLcNullNode.INSTANCE)).isTrue();
    }

    @Test
    void isNullNodeShouldReturnFalseForNull() {
        assertThat(ZLcNullNode.isNullNode(null)).isFalse();
    }

    @Test
    void isNullNodeShouldReturnFalseForOtherObjects() {
        assertThat(ZLcNullNode.isNullNode("value")).isFalse();
        assertThat(ZLcNullNode.isNullNode(123)).isFalse();
        assertThat(ZLcNullNode.isNullNode(new Object())).isFalse();
    }

    @Test
    void isKeyExistShouldReturnFalseForInstance() {
        assertThat(ZLcNullNode.isKeyExist(ZLcNullNode.INSTANCE)).isFalse();
    }

    @Test
    void isKeyExistShouldReturnTrueForNull() {
        assertThat(ZLcNullNode.isKeyExist(null)).isTrue();
    }

    @Test
    void isKeyExistShouldReturnTrueForOtherObjects() {
        assertThat(ZLcNullNode.isKeyExist("value")).isTrue();
        assertThat(ZLcNullNode.isKeyExist(0)).isTrue();
        assertThat(ZLcNullNode.isKeyExist(new Object())).isTrue();
    }

    @Test
    void toStringShouldReturnNullNodeString() {
        assertThat(ZLcNullNode.INSTANCE.toString()).isEqualTo("NULL_NODE");
    }

    @Test
    void instanceShouldBeSameAcrossCalls() {
        ZLcNullNode first = ZLcNullNode.INSTANCE;
        ZLcNullNode second = ZLcNullNode.INSTANCE;
        assertThat(first).isSameAs(second);
    }

    @Test
    void isNullNodeAndIsKeyExistShouldBeComplementary() {
        Object[] testValues = new Object[]{
            ZLcNullNode.INSTANCE,
            null,
            "string",
            123,
            new Object()
        };

        for (Object value : testValues) {
            assertThat(ZLcNullNode.isNullNode(value))
                .as("isNullNode for value: %s", value)
                .isNotEqualTo(ZLcNullNode.isKeyExist(value));
        }
    }
}