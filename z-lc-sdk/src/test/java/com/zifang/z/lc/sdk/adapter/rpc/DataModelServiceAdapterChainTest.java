package com.zifang.z.lc.sdk.adapter.rpc;

import com.zifang.z.lc.sdk.adapter.AbstractRpcDataModelServiceAdapter;
import com.zifang.z.lc.sdk.adapter.ServiceAdapter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DataModelServiceAdapter RPC 适配器链单元测试
 * <p>
 * 覆盖: AbstractRpcDataModelServiceAdapter (抽象基类, version appCode_modelCode 三段模板)
 */
public class DataModelServiceAdapterChainTest {

    @Test
    public void abstractRpcDataModelShouldBeAbstract() {
        assertTrue("AbstractRpcDataModelServiceAdapter 应当是抽象类",
                Modifier.isAbstract(AbstractRpcDataModelServiceAdapter.class.getModifiers()));
    }

    @Test
    public void abstractRpcDataModelShouldImplementServiceAdapter() {
        assertTrue("应当实现 ServiceAdapter",
                ServiceAdapter.class.isAssignableFrom(AbstractRpcDataModelServiceAdapter.class));
    }

    @Test
    public void versionShouldFormatWithAppCodeAndModelCode() throws Exception {
        Method m = AbstractRpcDataModelServiceAdapter.class.getMethod("version", String.class, String.class);
        String result = (String) m.invoke(null, "crm", "order");
        assertEquals("1.0.0_crm_order", result);
    }

    @Test
    public void versionShouldHandleNullAppCode() throws Exception {
        Method m = AbstractRpcDataModelServiceAdapter.class.getMethod("version", String.class, String.class);
        String result = (String) m.invoke(null, null, "order");
        assertEquals("1.0.0_null_order", result);
    }

    @Test
    public void versionShouldHandleNullModelCode() throws Exception {
        Method m = AbstractRpcDataModelServiceAdapter.class.getMethod("version", String.class, String.class);
        String result = (String) m.invoke(null, "crm", null);
        assertEquals("1.0.0_crm_null", result);
    }

    @Test
    public void versionShouldHandleBothNull() throws Exception {
        Method m = AbstractRpcDataModelServiceAdapter.class.getMethod("version", String.class, String.class);
        String result = (String) m.invoke(null, (String) null, (String) null);
        assertEquals("1.0.0_null_null", result);
    }

    @Test
    public void abstractRpcDataModelShouldDeclareCoreMethods() throws NoSuchMethodException {
        String[] methods = {"save", "queryPageable", "delete", "queryById", "queryList", "init", "dataCopy", "aiInit"};
        for (String name : methods) {
            boolean found = false;
            for (Method m : AbstractRpcDataModelServiceAdapter.class.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            assertTrue("应当声明方法: " + name, found);
        }
    }
}
