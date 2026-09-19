package com.zifang.z.lc.common.validation;

import org.junit.jupiter.api.Test;

import javax.validation.groups.Default;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcUpdate 单元测试
 *
 * @author zifang
 */
class ZLcUpdateTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcUpdate.class.isInterface()).isTrue();
    }

    @Test
    void shouldExtendDefault() {
        assertThat(Default.class.isAssignableFrom(ZLcUpdate.class)).isTrue();
    }

    @Test
    void shouldBeAssignableFromItself() {
        assertThat(ZLcUpdate.class.isAssignableFrom(ZLcUpdate.class)).isTrue();
    }
}