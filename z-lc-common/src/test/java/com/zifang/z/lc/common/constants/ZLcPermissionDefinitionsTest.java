package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPermissionDefinitions 单元测试
 *
 * @author zifang
 */
class ZLcPermissionDefinitionsTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcPermissionDefinitions.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveProductCode() {
        assertThat(ZLcPermissionDefinitions.PRODUCT_CODE).isEqualTo("z-lc");
    }

    @Test
    void shouldHaveAppManagerPermission() {
        assertThat(ZLcPermissionDefinitions.APP_MANAGER).isEqualTo("z-lc:app:manager");
    }
}