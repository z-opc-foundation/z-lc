package com.zifang.z.lc.design.config;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcDesignKnife4jConfig 单元测试
 */
public class ZLcDesignKnife4jConfigTest {

    @Test
    public void shouldBePublicClass() {
        assertTrue(Modifier.isPublic(ZLcDesignKnife4jConfig.class.getModifiers()));
    }

    @Test
    public void shouldHaveConfigurationAnnotation() {
        assertTrue(ZLcDesignKnife4jConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.design.config",
                ZLcDesignKnife4jConfig.class.getPackage().getName());
    }

    @Test
    public void shouldBeInstantiable() {
        ZLcDesignKnife4jConfig instance = new ZLcDesignKnife4jConfig();
        assertNotNull(instance);
    }

    @Test
    public void shouldNotBeInterfaceOrEnum() {
        assertFalse(ZLcDesignKnife4jConfig.class.isInterface());
        assertFalse(ZLcDesignKnife4jConfig.class.isEnum());
    }

    @Test
    public void shouldInheritFromObject() {
        assertEquals("java.lang.Object",
                ZLcDesignKnife4jConfig.class.getSuperclass().getName());
    }

    @Test
    public void shouldHaveOneDeclaredFieldForLogger() {
        // 包含 static Logger log 字段
        assertEquals(1, ZLcDesignKnife4jConfig.class.getDeclaredFields().length);
    }

    @Test
    public void classShouldNotBeFinal() {
        assertFalse(Modifier.isFinal(ZLcDesignKnife4jConfig.class.getModifiers()));
    }

    @Test
    public void shouldSupportMultipleInstantiations() {
        ZLcDesignKnife4jConfig a = new ZLcDesignKnife4jConfig();
        ZLcDesignKnife4jConfig b = new ZLcDesignKnife4jConfig();
        assertNotNull(a);
        assertNotNull(b);
        assertNotSame(a, b);
    }

    private static void assertNotSame(Object a, Object b) {
        org.junit.Assert.assertNotSame(a, b);
    }

    @Test
    public void classLoaderShouldNotBeNull() {
        assertNotNull(ZLcDesignKnife4jConfig.class.getClassLoader());
    }

    @Test
    public void declaredAnnotationsShouldIncludeConfiguration() {
        java.lang.annotation.Annotation[] anns =
                ZLcDesignKnife4jConfig.class.getDeclaredAnnotations();
        boolean found = false;
        for (java.lang.annotation.Annotation a : anns) {
            if (a instanceof org.springframework.context.annotation.Configuration) {
                found = true;
                break;
            }
        }
        assertTrue("应当标注 @Configuration", found);
    }

    @Test
    public void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(
                ZLcDesignKnife4jConfig.class.getDeclaredConstructor().getModifiers()));
    }
}