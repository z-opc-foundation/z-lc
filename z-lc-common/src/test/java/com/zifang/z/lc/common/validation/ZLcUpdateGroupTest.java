package com.zifang.z.lc.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcUpdateGroup 单元测试
 *
 * @author zifang
 */
class ZLcUpdateGroupTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcUpdateGroup.class.isInterface()).isTrue();
    }

    @Test
    void shouldBeAssignableFromItself() {
        assertThat(ZLcUpdateGroup.class.isAssignableFrom(ZLcUpdateGroup.class)).isTrue();
    }
}