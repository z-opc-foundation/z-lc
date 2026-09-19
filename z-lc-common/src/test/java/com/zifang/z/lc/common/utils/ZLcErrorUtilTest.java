package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.exception.ZLcPermissionLimitException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcErrorUtil 单元测试
 *
 * @author zifang
 */
class ZLcErrorUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcErrorUtil> constructor = ZLcErrorUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldNotThrowWhenConditionIsFalse() {
        ZLcErrorUtil.check(false, "Should not throw");
        // 如果没有抛出异常，测试通过
    }

    @Test
    void shouldThrowWhenConditionIsTrue() {
        assertThatThrownBy(() -> ZLcErrorUtil.check(true, "Validation failed"))
                .isInstanceOf(ZLcPermissionLimitException.class)
                .hasMessage("Validation failed");
    }

    @Test
    void shouldNotThrowWhenConditionIsFalseWithErrorLog() {
        ZLcErrorUtil.check(false, "Should not throw", "error log");
        // 如果没有抛出异常，测试通过
    }

    @Test
    void shouldThrowWhenConditionIsTrueWithErrorLog() {
        assertThatThrownBy(() -> ZLcErrorUtil.check(true, "Validation failed", "error log"))
                .isInstanceOf(ZLcPermissionLimitException.class)
                .hasMessage("Validation failed");
    }

    @Test
    void shouldThrowWhenConditionIsTrueWithNullErrorLog() {
        assertThatThrownBy(() -> ZLcErrorUtil.check(true, "Validation failed", null))
                .isInstanceOf(ZLcPermissionLimitException.class)
                .hasMessage("Validation failed");
    }

    @Test
    void shouldNotThrowWhenObjectIsNonNull() {
        ZLcErrorUtil.requireNonNull("not null", "Object should not be null");
        // 如果没有抛出异常，测试通过
    }

    @Test
    void shouldThrowWhenObjectIsNull() {
        assertThatThrownBy(() -> ZLcErrorUtil.requireNonNull(null, "Object is null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object is null");
    }

    @Test
    void shouldAllowMultipleChecks() {
        // 第一个检查通过
        ZLcErrorUtil.check(false, "First check");
        // 第二个检查通过
        ZLcErrorUtil.check(false, "Second check");
        // 第三个检查抛出异常
        assertThatThrownBy(() -> ZLcErrorUtil.check(true, "Third check"))
                .isInstanceOf(ZLcPermissionLimitException.class);
    }
}
