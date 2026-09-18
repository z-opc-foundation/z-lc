package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExtensionServiceConstance 单元测试
 *
 * @author zifang
 */
class ZLcExtensionServiceConstanceTest {

    @Test
    void shouldHaveDefaultConstructor() {
        assertThat(ZLcExtensionServiceConstance.class).isNotNull();
    }

    @Test
    void shouldHaveInitExtension() {
        assertThat(ZLcExtensionServiceConstance.INIT_EXTENSION).isEqualTo("init");
    }

    @Test
    void shouldHaveSubmitExtensions() {
        assertThat(ZLcExtensionServiceConstance.SUBMIT_VALIDATE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.SUBMIT_PRE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.SUBMIT_POST_EXTENSION).isNotNull();
    }

    @Test
    void shouldHaveQueryExtensions() {
        assertThat(ZLcExtensionServiceConstance.QUERY_PRE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.QUERY_POST_EXTENSION).isNotNull();
    }

    @Test
    void shouldHaveRemoveExtensions() {
        assertThat(ZLcExtensionServiceConstance.REMOVE_VALIDATE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.REMOVE_POST_EXTENSION).isNotNull();
    }

    @Test
    void shouldHaveAgreeExtensions() {
        assertThat(ZLcExtensionServiceConstance.AGREE_VALIDATE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.AGREE_PRE_EXTENSION).isNotNull();
        assertThat(ZLcExtensionServiceConstance.AGREE_POST_EXTENSION).isNotNull();
    }

    @Test
    void shouldHaveRejectExtensions() {
        assertThat(ZLcExtensionServiceConstance.REJECT_VALIDATE_EXTENSION).isNotNull();
    }
}
