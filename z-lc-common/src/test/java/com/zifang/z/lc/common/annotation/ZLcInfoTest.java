package com.zifang.z.lc.common.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcInfo 单元测试
 *
 * @author zifang
 */
class ZLcInfoTest {

    @Test
    void shouldHaveDocumentedAnnotation() {
        assertThat(ZLcInfo.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    @Test
    void shouldHaveRuntimeRetention() {
        assertThat(ZLcInfo.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetType() {
        Target target = ZLcInfo.class.getAnnotation(Target.class);
        assertThat(target.value()).hasSize(1);
        assertThat(target.value()[0]).isEqualTo(java.lang.annotation.ElementType.TYPE);
    }

    @Test
    void shouldHaveNameMethod() throws NoSuchMethodException {
        assertThat(ZLcInfo.class.getMethod("name")).isNotNull();
    }

    @Test
    void shouldHaveDescMethod() throws NoSuchMethodException {
        assertThat(ZLcInfo.class.getMethod("desc")).isNotNull();
    }

    @Test
    void shouldReturnTypeBeString() throws NoSuchMethodException {
        assertThat(ZLcInfo.class.getMethod("name").getReturnType()).isEqualTo(String.class);
        assertThat(ZLcInfo.class.getMethod("desc").getReturnType()).isEqualTo(String.class);
    }
}
