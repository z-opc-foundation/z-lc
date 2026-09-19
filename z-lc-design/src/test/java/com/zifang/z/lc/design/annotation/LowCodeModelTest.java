package com.zifang.z.lc.design.annotation;

import org.junit.Test;

import java.lang.annotation.Annotation;
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
 * LowCodeModel 注解单元测试
 *
 * @author zifang
 */
public class LowCodeModelTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(LowCodeModel.class.isAnnotation());
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        Retention retention = LowCodeModel.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldTargetType() {
        Target target = LowCodeModel.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType t : target.value()) {
            if (t == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("Should target TYPE", hasType);
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(LowCodeModel.class.getAnnotation(Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(LowCodeModel.class.getAnnotation(Inherited.class));
    }

    @Test
    public void shouldBeApplicableToClass() {
        assertTrue("Should have at least TYPE target",
                LowCodeModel.class.isAnnotationPresent(Target.class));
    }

    @Test
    public void annotationUsageShouldCompile() {
        @LowCodeModel
        class TestClass {
        }
        Annotation[] annotations = TestClass.class.getAnnotations();
        boolean hasLowCodeModel = false;
        for (Annotation a : annotations) {
            if (a instanceof LowCodeModel) {
                hasLowCodeModel = true;
                break;
            }
        }
        assertTrue("TestClass should have @LowCodeModel", hasLowCodeModel);
    }

    @Test
    public void annotationShouldBeInherited() {
        // Inherited annotation can be inherited by subclasses
        assertNotNull(LowCodeModel.class.getAnnotation(Inherited.class));
    }

    private static class ParentClass {
    }

    @LowCodeModel
    private static class AnnotatedParent {
    }

    private static class ChildClass extends AnnotatedParent {
    }

    @Test
    public void inheritedAnnotationShouldPropagate() {
        Annotation[] annotations = ChildClass.class.getAnnotations();
        boolean hasLowCodeModel = false;
        for (Annotation a : annotations) {
            if (a instanceof LowCodeModel) {
                hasLowCodeModel = true;
                break;
            }
        }
        // @Inherited means subclasses inherit the annotation from parent
        assertTrue("Subclass should inherit @LowCodeModel", hasLowCodeModel);
    }
}