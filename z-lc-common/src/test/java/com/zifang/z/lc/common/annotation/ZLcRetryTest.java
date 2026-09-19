package com.zifang.z.lc.common.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcRetry 单元测试
 *
 * @author zifang
 */
class ZLcRetryTest {

    @Test
    void shouldHaveDocumentedAnnotation() {
        assertThat(ZLcRetry.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    @Test
    void shouldHaveRuntimeRetention() {
        assertThat(ZLcRetry.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldHaveInheritedAnnotation() {
        assertThat(ZLcRetry.class.isAnnotationPresent(Inherited.class)).isTrue();
    }

    @Test
    void shouldTargetMethod() {
        Target target = ZLcRetry.class.getAnnotation(Target.class);
        assertThat(target.value()).hasSize(1);
        assertThat(target.value()[0]).isEqualTo(ElementType.METHOD);
    }

    @Test
    void shouldHaveRetryAttemptsMethod() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("retryAttempts")).isNotNull();
    }

    @Test
    void shouldHaveSleepIntervalMethod() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("sleepInterval")).isNotNull();
    }

    @Test
    void shouldHaveIgnoreExceptionsMethod() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("ignoreExceptions")).isNotNull();
    }

    @Test
    void shouldRetryAttemptsReturnTypeBeInt() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("retryAttempts").getReturnType()).isEqualTo(int.class);
    }

    @Test
    void shouldSleepIntervalReturnTypeBeLong() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("sleepInterval").getReturnType()).isEqualTo(long.class);
    }

    @Test
    void shouldIgnoreExceptionsReturnTypeBeClassArray() throws NoSuchMethodException {
        assertThat(ZLcRetry.class.getMethod("ignoreExceptions").getReturnType()).isEqualTo(Class[].class);
    }

    @Test
    void shouldRetryAttemptsDefaultValueBeThree() throws NoSuchMethodException {
        int defaultValue = (int) ZLcRetry.class.getMethod("retryAttempts").getDefaultValue();
        assertThat(defaultValue).isEqualTo(3);
    }

    @Test
    void shouldSleepIntervalDefaultValueBeFiveSeconds() throws NoSuchMethodException {
        long defaultValue = (long) ZLcRetry.class.getMethod("sleepInterval").getDefaultValue();
        assertThat(defaultValue).isEqualTo(5000L);
    }

    @Test
    void shouldIgnoreExceptionsDefaultValueBeEmpty() throws NoSuchMethodException {
        Class<?>[] defaultValue = (Class<?>[]) ZLcRetry.class.getMethod("ignoreExceptions").getDefaultValue();
        assertThat(defaultValue).isEmpty();
    }
}
