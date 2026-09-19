package com.zifang.z.lc.common.aspect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcRetryAspect 单元测试
 *
 * @author zifang
 */
class ZLcRetryAspectTest {

    @Test
    void shouldBeAnnotatedAsAspect() {
        assertThat(ZLcRetryAspect.class.isAnnotationPresent(org.aspectj.lang.annotation.Aspect.class)).isTrue();
    }

    @Test
    void shouldBeAnnotatedAsComponent() {
        assertThat(ZLcRetryAspect.class.isAnnotationPresent(org.springframework.stereotype.Component.class)).isTrue();
    }

    @Test
    void shouldCreateInstance() {
        ZLcRetryAspect aspect = new ZLcRetryAspect();
        assertThat(aspect).isNotNull();
    }

    @Test
    void shouldHavePointcutAnnotation() throws NoSuchMethodException {
        assertThat(ZLcRetryAspect.class.getMethod("pointcut", org.aspectj.lang.ProceedingJoinPoint.class)
                .isAnnotationPresent(org.aspectj.lang.annotation.Around.class)).isTrue();
    }

    @Test
    void shouldHaveRetryableExecuteMethod() throws NoSuchMethodException {
        assertThat(ZLcRetryAspect.class.getDeclaredMethod("retryableExecute",
                org.aspectj.lang.ProceedingJoinPoint.class)).isNotNull();
    }
}