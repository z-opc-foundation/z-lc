package com.zifang.z.lc.common.validation;

import org.junit.jupiter.api.Test;

import javax.validation.groups.Default;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCreate 单元测试
 *
 * @author zifang
 */
class ZLcCreateTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcCreate.class.isInterface()).isTrue();
    }

    @Test
    void shouldExtendDefault() {
        assertThat(Default.class.isAssignableFrom(ZLcCreate.class)).isTrue();
    }

    @Test
    void shouldBeAssignableFromItself() {
        assertThat(ZLcCreate.class.isAssignableFrom(ZLcCreate.class)).isTrue();
    }
}