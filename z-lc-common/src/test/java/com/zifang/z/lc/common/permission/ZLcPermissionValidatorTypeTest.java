package com.zifang.z.lc.common.permission;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPermissionValidatorType 单元测试
 *
 * @author zifang
 */
class ZLcPermissionValidatorTypeTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcPermissionValidatorType> constructor = ZLcPermissionValidatorType.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveAllConstant() {
        assertThat(ZLcPermissionValidatorType.ALL).isEqualTo("all");
    }
}
