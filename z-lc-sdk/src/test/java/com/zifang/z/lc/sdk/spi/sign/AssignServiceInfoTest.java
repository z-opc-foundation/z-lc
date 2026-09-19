package com.zifang.z.lc.sdk.spi.sign;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * AssignServiceInfo 单元测试
 *
 * @author zifang
 */
public class AssignServiceInfoTest {

    @Test
    public void shouldBeAnnotation() {
        assertTrue(AssignServiceInfo.class.isAnnotation());
    }

    @Test
    public void shouldHaveRuntimeRetention() {
        Retention retention = AssignServiceInfo.class.getAnnotation(Retention.class);
        assertTrue(retention != null);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void shouldTargetType() {
        Target target = AssignServiceInfo.class.getAnnotation(Target.class);
        assertTrue(target != null);
        assertEquals(1, target.value().length);
        assertEquals(ElementType.TYPE, target.value()[0]);
    }

    @Test
    public void shouldHaveIdentityCodeMethod() throws NoSuchMethodException {
        assertTrue(AssignServiceInfo.class.getMethod("identityCode") != null);
    }

    @Test
    public void shouldHaveExpressionMethod() throws NoSuchMethodException {
        assertTrue(AssignServiceInfo.class.getMethod("expression") != null);
    }

    @Test
    public void shouldHaveExportRpcMethod() throws NoSuchMethodException {
        assertTrue(AssignServiceInfo.class.getMethod("exportRpc") != null);
    }

    @Test
    public void shouldHaveDefaultExpressionValue() throws NoSuchMethodException {
        assertEquals("", AssignServiceInfo.class.getMethod("expression").getDefaultValue());
    }

    @Test
    public void shouldHaveDefaultExportRpcValue() throws NoSuchMethodException {
        assertEquals(false, AssignServiceInfo.class.getMethod("exportRpc").getDefaultValue());
    }
}