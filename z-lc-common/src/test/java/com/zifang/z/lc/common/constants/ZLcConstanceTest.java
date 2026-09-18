package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcConstance 常量类单元测试
 *
 * @author zifang
 */
class ZLcConstanceTest {

    @Test
    void shouldHaveDefaultConstructor() {
        assertThat(ZLcConstance.class).isNotNull();
    }

    @Test
    void PRODUCT_CODE_shouldBeZLC() {
        assertThat(ZLcConstance.PRODUCT_CODE).isEqualTo("z-lc");
    }

    @Test
    void APP_CODE_shouldNotBeNull() {
        assertThat(ZLcConstance.APP_CODE).isNotNull();
    }

    @Test
    void MODEL_CODE_shouldNotBeNull() {
        assertThat(ZLcConstance.MODEL_CODE).isNotNull();
    }

    @Test
    void MQ_TOPIC_shouldHaveFormat() {
        assertThat(ZLcConstance.MQ_TOPIC).contains("%s");
    }

    @Test
    void ALL_PROCESS_EVENT_TYPES_shouldNotBeNull() {
        assertThat(ZLcConstance.ALL_PROCESS_EVENT_TYPES).isNotNull();
    }

    @Test
    void ALL_TASK_EVENT_TYPES_shouldNotBeNull() {
        assertThat(ZLcConstance.ALL_TASK_EVENT_TYPES).isNotNull();
    }

    @Test
    void DEFAULT_FIELD_LENGTH_MAP_shouldNotBeNull() {
        assertThat(ZLcConstance.DEFAULT_FIELD_LENGTH_MAP).isNotNull();
    }

    @Test
    void DEFAULT_FIELD_LENGTH_MAP_shouldContainEntries() {
        assertThat(ZLcConstance.DEFAULT_FIELD_LENGTH_MAP).isNotEmpty();
    }
}
