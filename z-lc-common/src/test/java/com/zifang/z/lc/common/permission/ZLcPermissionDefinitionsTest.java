package com.zifang.z.lc.common.permission;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPermissionDefinitions (permission包版本) 单元测试
 *
 * @author zifang
 */
class ZLcPermissionDefinitionsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcPermissionDefinitions> constructor = ZLcPermissionDefinitions.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveProductCode() {
        assertThat(ZLcPermissionDefinitions.PRODUCT_CODE).isEqualTo("z-lc");
    }

    @Test
    void shouldHavePlatformAppManagerPermission() {
        assertThat(ZLcPermissionDefinitions.PLATFORM_APP_MANAGER).isEqualTo("z-lc:platform:app:manager");
    }
}