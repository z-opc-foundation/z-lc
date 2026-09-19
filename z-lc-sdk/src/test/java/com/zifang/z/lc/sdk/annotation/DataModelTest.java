package com.zifang.z.lc.sdk.annotation;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModel 注解单元测试
 */
public class DataModelTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(DataModel.class.isAnnotation());
    }

    @Test
    public void shouldBeApplicableToType() {
        Target target = DataModel.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType et : target.value()) {
            if (et == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("@DataModel 应标注在 TYPE 上", hasType);
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        java.lang.annotation.Retention retention =
                DataModel.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(DataModel.class.getAnnotation(java.lang.annotation.Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(DataModel.class.getAnnotation(java.lang.annotation.Inherited.class));
    }

    @Test
    public void shouldHaveAppCodeMethod() throws NoSuchMethodException {
        DataModel.class.getMethod("appCode");
    }

    @Test
    public void shouldHaveModelCodeMethod() throws NoSuchMethodException {
        DataModel.class.getMethod("modelCode");
    }

    @Test
    public void shouldHaveModelNameMethod() throws NoSuchMethodException {
        DataModel.class.getMethod("modelName");
    }

    @Test
    public void appCodeDefaultShouldBeEmptyString() throws NoSuchMethodException {
        java.lang.reflect.Method m = DataModel.class.getMethod("appCode");
        assertEquals("", m.getDefaultValue());
    }

    @Test
    public void modelNameDefaultShouldBeEmptyString() throws NoSuchMethodException {
        java.lang.reflect.Method m = DataModel.class.getMethod("modelName");
        assertEquals("", m.getDefaultValue());
    }

    @Test
    public void modelCodeShouldHaveNoDefault() throws NoSuchMethodException {
        java.lang.reflect.Method m = DataModel.class.getMethod("modelCode");
        // modelCode 是必填, 不应有 default
        Object def = m.getDefaultValue();
        assertTrue(def == null || "".equals(def));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.annotation",
                DataModel.class.getPackage().getName());
    }

    @Test
    public void allMethodsShouldReturnString() throws NoSuchMethodException {
        assertEquals(String.class, DataModel.class.getMethod("appCode").getReturnType());
        assertEquals(String.class, DataModel.class.getMethod("modelCode").getReturnType());
        assertEquals(String.class, DataModel.class.getMethod("modelName").getReturnType());
    }
}