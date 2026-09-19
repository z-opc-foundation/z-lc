package com.zifang.z.lc.design.annotation;

import org.junit.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * LowCodeModelService 注解单元测试
 *
 * @author zifang
 */
public class LowCodeModelServiceTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(LowCodeModelService.class.isAnnotation());
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        Retention retention = LowCodeModelService.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldTargetType() {
        Target target = LowCodeModelService.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType t : target.value()) {
            if (t == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue(hasType);
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(LowCodeModelService.class.getAnnotation(Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(LowCodeModelService.class.getAnnotation(Inherited.class));
    }

    @Test
    public void shouldBeMarkedWithService() {
        // Should also be @Service (Spring stereotype)
        assertNotNull(LowCodeModelService.class.getAnnotation(
                org.springframework.stereotype.Service.class));
    }

    @Test
    public void annotationUsageShouldCompile() {
        @LowCodeModelService
        class TestClass {
        }
        boolean found = false;
        for (java.lang.annotation.Annotation a : TestClass.class.getAnnotations()) {
            if (a instanceof LowCodeModelService) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }
}