package com.zifang.z.lc.common.aspect;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataCorrectRecord 单元测试
 *
 * @author zifang
 */
class ZLcDataCorrectRecordTest {

    @Test
    void shouldHaveDocumentedAnnotation() {
        assertThat(ZLcDataCorrectRecord.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    @Test
    void shouldHaveRuntimeRetention() {
        assertThat(ZLcDataCorrectRecord.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetParameterAndMethod() {
        Target target = ZLcDataCorrectRecord.class.getAnnotation(Target.class);
        assertThat(target.value()).hasSize(2);
        assertThat(target.value()).contains(java.lang.annotation.ElementType.PARAMETER);
        assertThat(target.value()).contains(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void shouldHaveActionMethod() throws NoSuchMethodException {
        assertThat(ZLcDataCorrectRecord.class.getMethod("action")).isNotNull();
    }

    @Test
    void shouldReturnTypeBeString() throws NoSuchMethodException {
        assertThat(ZLcDataCorrectRecord.class.getMethod("action").getReturnType()).isEqualTo(String.class);
    }
}
