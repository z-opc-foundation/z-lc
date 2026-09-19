package com.zifang.z.lc.sdk.annotation;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelServiceInfo 注解单元测试
 */
public class DataModelServiceInfoTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(DataModelServiceInfo.class.isAnnotation());
    }

    @Test
    public void shouldBeApplicableToType() {
        Target target = DataModelServiceInfo.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType et : target.value()) {
            if (et == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("@DataModelServiceInfo 应标注在 TYPE 上", hasType);
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        java.lang.annotation.Retention retention =
                DataModelServiceInfo.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldBeDocumented() {
        assertNotNull(DataModelServiceInfo.class.getAnnotation(java.lang.annotation.Documented.class));
    }

    @Test
    public void shouldBeInherited() {
        assertNotNull(DataModelServiceInfo.class.getAnnotation(java.lang.annotation.Inherited.class));
    }

    @Test
    public void shouldHaveAppCodeMethod() throws NoSuchMethodException {
        DataModelServiceInfo.class.getMethod("appCode");
    }

    @Test
    public void shouldHaveModelCodeMethod() throws NoSuchMethodException {
        DataModelServiceInfo.class.getMethod("modelCode");
    }

    @Test
    public void shouldHaveExpressionMethod() throws NoSuchMethodException {
        DataModelServiceInfo.class.getMethod("expression");
    }

    @Test
    public void shouldHaveExportRpcMethod() throws NoSuchMethodException {
        DataModelServiceInfo.class.getMethod("exportRpc");
    }

    @Test
    public void appCodeDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", DataModelServiceInfo.class.getMethod("appCode").getDefaultValue());
    }

    @Test
    public void modelCodeDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", DataModelServiceInfo.class.getMethod("modelCode").getDefaultValue());
    }

    @Test
    public void expressionDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", DataModelServiceInfo.class.getMethod("expression").getDefaultValue());
    }

    @Test
    public void exportRpcDefaultShouldBeFalse() throws NoSuchMethodException {
        java.lang.reflect.Method m = DataModelServiceInfo.class.getMethod("exportRpc");
        assertEquals(Boolean.FALSE, m.getDefaultValue());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.annotation",
                DataModelServiceInfo.class.getPackage().getName());
    }

    @Test
    public void shouldBeInterface() {
        assertTrue(DataModelServiceInfo.class.isInterface());
    }

    @Test
    public void shouldNotBeEnumOrClass() {
        assertFalse(DataModelServiceInfo.class.isEnum());
        assertFalse(DataModelServiceInfo.class.isAnnotationPresent(java.lang.annotation.Documented.class) == false);
    }
}