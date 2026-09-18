package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCacheConstance 单元测试
 *
 * @author zifang
 */
class ZLcCacheConstanceTest {

    @Test
    void TABLE_RESULT_SET_CACHE_KEY_PREFIX_shouldNotBeNull() {
        assertThat(ZLcCacheConstance.TABLE_RESULT_SET_CACHE_KEY_PREFIX).isNotNull();
    }

    @Test
    void TABLE_RESULT_SET_CACHE_KEY_PREFIX_shouldStartWithTABLE() {
        assertThat(ZLcCacheConstance.TABLE_RESULT_SET_CACHE_KEY_PREFIX).startsWith("TABLE_RESULT_SET_CACHE:");
    }

    @Test
    void TABLE_RESULT_SET_CACHE_KEY_PREFIX_shouldEndWithColon() {
        assertThat(ZLcCacheConstance.TABLE_RESULT_SET_CACHE_KEY_PREFIX).endsWith(":");
    }
}
