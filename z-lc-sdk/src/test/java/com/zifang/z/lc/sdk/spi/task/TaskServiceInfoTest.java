package com.zifang.z.lc.sdk.spi.task;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TaskServiceInfo 注解单元测试
 */
public class TaskServiceInfoTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(TaskServiceInfo.class.isAnnotation());
    }

    @Test
    public void shouldBeApplicableToType() {
        Target target = TaskServiceInfo.class.getAnnotation(Target.class);
        assertNotNull(target);
        boolean hasType = false;
        for (ElementType et : target.value()) {
            if (et == ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue("@TaskServiceInfo 应标注在 TYPE 上", hasType);
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        java.lang.annotation.Retention retention =
                TaskServiceInfo.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldHaveIdentityCodeMethod() throws NoSuchMethodException {
        TaskServiceInfo.class.getMethod("identityCode");
    }

    @Test
    public void shouldHaveExpressionMethod() throws NoSuchMethodException {
        TaskServiceInfo.class.getMethod("expression");
    }

    @Test
    public void shouldHaveExportRpcMethod() throws NoSuchMethodException {
        TaskServiceInfo.class.getMethod("exportRpc");
    }

    @Test
    public void identityCodeShouldHaveNoDefault() throws NoSuchMethodException {
        java.lang.reflect.Method m = TaskServiceInfo.class.getMethod("identityCode");
        Object def = m.getDefaultValue();
        assertTrue("identityCode 是必填, 应无默认值或为空", def == null || "".equals(def));
    }

    @Test
    public void expressionDefaultShouldBeEmpty() throws NoSuchMethodException {
        assertEquals("", TaskServiceInfo.class.getMethod("expression").getDefaultValue());
    }

    @Test
    public void exportRpcDefaultShouldBeFalse() throws NoSuchMethodException {
        assertEquals(Boolean.FALSE, TaskServiceInfo.class.getMethod("exportRpc").getDefaultValue());
    }

    @Test
    public void identityCodeReturnTypeShouldBeString() throws NoSuchMethodException {
        assertEquals(String.class, TaskServiceInfo.class.getMethod("identityCode").getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.task",
                TaskServiceInfo.class.getPackage().getName());
    }

    @Test
    public void shouldNotBeDocumented() {
        // TaskServiceInfo 没有 @Documented 标注
        assertFalse(TaskServiceInfo.class.isAnnotationPresent(java.lang.annotation.Documented.class));
    }
}