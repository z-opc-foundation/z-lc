package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebBuildSolutionConstant 单元测试
 *
 * @author zifang
 */
class ZLcWebBuildSolutionConstantTest {

    @Test
    void CODE_PREFIX_shouldBeSOLUTION() {
        assertThat(ZLcWebBuildSolutionConstant.CODE_PREFIX).isEqualTo("SOLUTION");
    }

    @Test
    void CODE_PREFIX_shouldNotBeEmpty() {
        assertThat(ZLcWebBuildSolutionConstant.CODE_PREFIX).isNotEmpty();
    }

    @Test
    void shouldHavePrivateConstructor() {
        // 验证类不能被实例化
        java.lang.reflect.Constructor<?>[] constructors = ZLcWebBuildSolutionConstant.class.getDeclaredConstructors();
        for (java.lang.reflect.Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                assertThat(constructor.isAccessible()).isFalse();
            }
        }
    }
}
