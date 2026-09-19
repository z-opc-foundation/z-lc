package com.zifang.z.lc.sdk.annotation;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * EnableModelService 注解单元测试
 */
public class EnableModelServiceTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(EnableModelService.class.isAnnotation());
    }

    @Test
    public void shouldBeApplicableToType() {
        Target target = EnableModelService.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType et : target.value()) {
            if (et == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("@EnableModelService 应标注在 TYPE 上", hasType);
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        java.lang.annotation.Retention retention =
                EnableModelService.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(EnableModelService.class.getAnnotation(java.lang.annotation.Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(EnableModelService.class.getAnnotation(java.lang.annotation.Inherited.class));
    }

    @Test
    public void shouldHaveBasePackagesMethod() throws NoSuchMethodException {
        EnableModelService.class.getMethod("basePackages");
    }

    @Test
    public void basePackagesReturnTypeShouldBeArray() throws NoSuchMethodException {
        java.lang.reflect.Method m = EnableModelService.class.getMethod("basePackages");
        assertEquals(String[].class, m.getReturnType());
    }

    @Test
    public void basePackagesDefaultShouldBeEmptyArray() throws NoSuchMethodException {
        java.lang.reflect.Method m = EnableModelService.class.getMethod("basePackages");
        Object def = m.getDefaultValue();
        assertNotNull("默认值应存在", def);
        assertTrue(def instanceof String[]);
        assertEquals(0, ((String[]) def).length);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.annotation",
                EnableModelService.class.getPackage().getName());
    }
}