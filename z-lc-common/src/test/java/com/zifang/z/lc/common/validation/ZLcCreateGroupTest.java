package com.zifang.z.lc.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCreateGroup 单元测试
 *
 * @author zifang
 */
class ZLcCreateGroupTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcCreateGroup.class.isInterface()).isTrue();
    }

    @Test
    void shouldBeAssignableFromItself() {
        assertThat(ZLcCreateGroup.class.isAssignableFrom(ZLcCreateGroup.class)).isTrue();
    }
}