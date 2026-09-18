package com.zifang.z.lc.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验组接口单元测试
 *
 * @author zifang
 */
class ZLcValidationTest {

    @Test
    void createGroup_shouldBeInterface() {
        assertThat(ZLcCreateGroup.class.isInterface()).isTrue();
    }

    @Test
    void updateGroup_shouldBeInterface() {
        assertThat(ZLcUpdateGroup.class.isInterface()).isTrue();
    }

    @Test
    void createGroup_shouldBeMarkerInterface() {
        // 验证是空接口（标记接口）
        assertThat(ZLcCreateGroup.class.getMethods()).isEmpty();
    }

    @Test
    void updateGroup_shouldBeMarkerInterface() {
        // 验证是空接口（标记接口）
        assertThat(ZLcUpdateGroup.class.getMethods()).isEmpty();
    }
}
