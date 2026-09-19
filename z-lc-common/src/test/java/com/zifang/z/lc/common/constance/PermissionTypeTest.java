package com.zifang.z.lc.common.constance;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionType 单元测试
 */
class PermissionTypeTest {

    @Test
    void shouldHaveMenuPermission() {
        assertThat(PermissionType.MENU).isEqualTo(1);
    }

    @Test
    void shouldHaveButtonPermission() {
        assertThat(PermissionType.BUTTON).isEqualTo(2);
    }

    @Test
    void shouldHaveApiPermission() {
        assertThat(PermissionType.API).isEqualTo(3);
    }

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<PermissionType> ctor = PermissionType.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
    }

    @Test
    void permissionsShouldBeDistinct() {
        assertThat(PermissionType.MENU).isNotEqualTo(PermissionType.BUTTON);
        assertThat(PermissionType.BUTTON).isNotEqualTo(PermissionType.API);
        assertThat(PermissionType.MENU).isNotEqualTo(PermissionType.API);
    }

    @Test
    void permissionsShouldBeIntegers() {
        assertThat(PermissionType.MENU).isInstanceOf(Integer.class);
        assertThat(PermissionType.BUTTON).isInstanceOf(Integer.class);
        assertThat(PermissionType.API).isInstanceOf(Integer.class);
    }
}