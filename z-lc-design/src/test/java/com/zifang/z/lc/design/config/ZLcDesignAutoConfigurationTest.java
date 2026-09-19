package com.zifang.z.lc.design.config;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcDesignAutoConfiguration 单元测试
 */
public class ZLcDesignAutoConfigurationTest {

    @Test
    public void shouldBePublicClass() {
        assertTrue(Modifier.isPublic(ZLcDesignAutoConfiguration.class.getModifiers()));
    }

    @Test
    public void shouldHaveConfigurationAnnotation() {
        assertTrue(ZLcDesignAutoConfiguration.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class));
    }

    @Test
    public void shouldHaveComponentScanAnnotation() {
        assertTrue(ZLcDesignAutoConfiguration.class.isAnnotationPresent(
                org.springframework.context.annotation.ComponentScan.class));
    }

    @Test
    public void shouldScanCorrectBasePackage() {
        org.springframework.context.annotation.ComponentScan cs =
                ZLcDesignAutoConfiguration.class.getAnnotation(
                        org.springframework.context.annotation.ComponentScan.class);
        assertNotNull(cs);
        boolean found = false;
        for (String pkg : cs.basePackages()) {
            if ("com.zifang.z.lc.design".equals(pkg)) {
                found = true;
                break;
            }
        }
        assertTrue("basePackages 应当包含 com.zifang.z.lc.design", found);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.design.config",
                ZLcDesignAutoConfiguration.class.getPackage().getName());
    }

    @Test
    public void shouldBeInstantiable() throws Exception {
        ZLcDesignAutoConfiguration instance = new ZLcDesignAutoConfiguration();
        assertNotNull(instance);
    }

    @Test
    public void classShouldNotBeFinal() {
        assertFalse(Modifier.isFinal(ZLcDesignAutoConfiguration.class.getModifiers()));
    }

    @Test
    public void shouldNotBeInterfaceOrEnum() {
        assertFalse(ZLcDesignAutoConfiguration.class.isInterface());
        assertFalse(ZLcDesignAutoConfiguration.class.isEnum());
    }

    @Test
    public void shouldInheritFromObject() {
        assertEquals("java.lang.Object",
                ZLcDesignAutoConfiguration.class.getSuperclass().getName());
    }

    @Test
    public void declaredAnnotationsShouldNotBeEmpty() {
        assertTrue(ZLcDesignAutoConfiguration.class.getDeclaredAnnotations().length > 0);
    }

    @Test
    public void classLoaderShouldNotBeNull() {
        assertNotNull(ZLcDesignAutoConfiguration.class.getClassLoader());
    }
}