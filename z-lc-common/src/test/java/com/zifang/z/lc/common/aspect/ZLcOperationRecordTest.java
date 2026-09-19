package com.zifang.z.lc.common.aspect;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcOperationRecord 单元测试 (aspect包)
 *
 * @author zifang
 */
class ZLcOperationRecordTest {

    @Test
    void shouldHaveDocumentedAnnotation() {
        assertThat(ZLcOperationRecord.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    @Test
    void shouldHaveRuntimeRetention() {
        assertThat(ZLcOperationRecord.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetParameterAndMethod() {
        Target target = ZLcOperationRecord.class.getAnnotation(Target.class);
        assertThat(target.value()).hasSize(2);
        assertThat(target.value()).contains(java.lang.annotation.ElementType.PARAMETER);
        assertThat(target.value()).contains(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void shouldHaveActionMethod() throws NoSuchMethodException {
        assertThat(ZLcOperationRecord.class.getMethod("action")).isNotNull();
    }

    @Test
    void shouldReturnTypeBeString() throws NoSuchMethodException {
        assertThat(ZLcOperationRecord.class.getMethod("action").getReturnType()).isEqualTo(String.class);
    }
}
