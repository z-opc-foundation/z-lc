package com.zifang.z.lc.common.permission;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPermissionValidator 单元测试
 *
 * @author zifang
 */
class ZLcPermissionValidatorTest {

    @Test
    void shouldBeAnnotation() {
        assertThat(ZLcPermissionValidator.class.isAnnotation()).isTrue();
    }

    @Test
    void shouldHaveDocumentedAnnotation() {
        assertThat(ZLcPermissionValidator.class.isAnnotationPresent(java.lang.annotation.Documented.class)).isTrue();
    }

    @Test
    void shouldHaveRuntimeRetention() {
        Retention retention = ZLcPermissionValidator.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetMethod() {
        Target target = ZLcPermissionValidator.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).contains(ElementType.METHOD);
    }

    @Test
    void shouldHaveValidateTypeMethod() throws NoSuchMethodException {
        assertThat(ZLcPermissionValidator.class.getMethod("validateType")).isNotNull();
    }

    @Test
    void shouldHavePermissionsMethod() throws NoSuchMethodException {
        assertThat(ZLcPermissionValidator.class.getMethod("permissions")).isNotNull();
    }
}