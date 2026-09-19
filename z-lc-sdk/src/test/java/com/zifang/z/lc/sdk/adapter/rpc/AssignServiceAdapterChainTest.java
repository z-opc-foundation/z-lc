package com.zifang.z.lc.sdk.adapter.rpc;

import com.zifang.z.lc.sdk.spi.sign.adapter.AssignServiceAdapter;
import com.zifang.z.lc.sdk.spi.sign.adapter.AbstractRpcAssignServiceAdapter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * AssignServiceAdapter RPC 适配器链单元测试
 * <p>
 * 覆盖: AssignServiceAdapter (接口) + AbstractRpcAssignServiceAdapter (抽象基类, version 二段模板)
 */
public class AssignServiceAdapterChainTest {

    @Test
    public void assignServiceAdapterShouldBeInterface() {
        assertTrue("AssignServiceAdapter 应当是接口", AssignServiceAdapter.class.isInterface());
    }

    @Test
    public void abstractRpcAssignShouldBeAbstract() {
        assertTrue("AbstractRpcAssignServiceAdapter 应当是抽象类",
                Modifier.isAbstract(AbstractRpcAssignServiceAdapter.class.getModifiers()));
    }

    @Test
    public void abstractRpcAssignShouldImplementAssignServiceAdapter() {
        assertTrue("应当实现 AssignServiceAdapter",
                AssignServiceAdapter.class.isAssignableFrom(AbstractRpcAssignServiceAdapter.class));
    }

    @Test
    public void versionShouldFormatCorrectly() throws Exception {
        Method m = AbstractRpcAssignServiceAdapter.class.getMethod("version", String.class);
        String result = (String) m.invoke(null, "order");
        assertEquals("1.0.0_order", result);
    }

    @Test
    public void versionShouldHandleNull() throws Exception {
        Method m = AbstractRpcAssignServiceAdapter.class.getMethod("version", String.class);
        String result = (String) m.invoke(null, (String) null);
        assertEquals("1.0.0_null", result);
    }

    @Test
    public void versionShouldHandleEmpty() throws Exception {
        Method m = AbstractRpcAssignServiceAdapter.class.getMethod("version", String.class);
        String result = (String) m.invoke(null, "");
        assertEquals("1.0.0_", result);
    }

    @Test
    public void abstractRpcAssignShouldDeclareAbstractMethods() throws NoSuchMethodException {
        // 验证接口中声明的核心方法在抽象类中可访问
        String[] methods = {"addSignJob", "querySignResult", "verifySignedData", "getSignedData"};
        for (String name : methods) {
            boolean found = false;
            // 在抽象类的所有方法（包括继承自接口的方法）中查找
            for (Method m : AbstractRpcAssignServiceAdapter.class.getMethods()) {
                if (m.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            assertTrue("应当声明方法: " + name, found);
        }
    }
}
