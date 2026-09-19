package com.zifang.z.lc.sdk.annotation;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * InterfaceMapping 注解单元测试
 */
public class InterfaceMappingTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(InterfaceMapping.class.isAnnotation());
    }

    @Test
    public void shouldBeApplicableToType() {
        Target target = InterfaceMapping.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType et : target.value()) {
            if (et == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("@InterfaceMapping 应标注在 TYPE 上", hasType);
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        java.lang.annotation.Retention retention =
                InterfaceMapping.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(InterfaceMapping.class.getAnnotation(java.lang.annotation.Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(InterfaceMapping.class.getAnnotation(java.lang.annotation.Inherited.class));
    }

    @Test
    public void shouldHaveNameMethod() throws NoSuchMethodException {
        InterfaceMapping.class.getMethod("name");
    }

    @Test
    public void shouldHaveCodeMethod() throws NoSuchMethodException {
        InterfaceMapping.class.getMethod("code");
    }

    @Test
    public void shouldHaveGroupMethod() throws NoSuchMethodException {
        InterfaceMapping.class.getMethod("group");
    }

    @Test
    public void nameDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", InterfaceMapping.class.getMethod("name").getDefaultValue());
    }

    @Test
    public void codeDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", InterfaceMapping.class.getMethod("code").getDefaultValue());
    }

    @Test
    public void groupDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", InterfaceMapping.class.getMethod("group").getDefaultValue());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.annotation",
                InterfaceMapping.class.getPackage().getName());
    }
}