package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFlagConstants 单元测试
 *
 * @author zifang
 */
class ZLcFlagConstantsTest {

    @Test
    void TRUE_shouldBeOne() {
        assertThat(ZLcFlagConstants.TRUE).isEqualTo(1);
    }

    @Test
    void FALSE_shouldBeZero() {
        assertThat(ZLcFlagConstants.FALSE).isEqualTo(0);
    }

    @Test
    void TRUE_shouldNotEqualFalse() {
        assertThat(ZLcFlagConstants.TRUE).isNotEqualTo(ZLcFlagConstants.FALSE);
    }
}
