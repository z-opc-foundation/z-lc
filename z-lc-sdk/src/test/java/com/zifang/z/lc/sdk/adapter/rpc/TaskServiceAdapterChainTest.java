package com.zifang.z.lc.sdk.adapter.rpc;

import com.zifang.z.lc.sdk.spi.task.adapter.TaskServiceAdapter;
import com.zifang.z.lc.sdk.spi.task.adapter.AbstractRpcTaskServiceAdapter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TaskServiceAdapter RPC 适配器链单元测试
 * <p>
 * 覆盖: TaskServiceAdapter (接口) + AbstractRpcTaskServiceAdapter (抽象基类, version 二段模板)
 */
public class TaskServiceAdapterChainTest {

    @Test
    public void taskServiceAdapterShouldBeInterface() {
        assertTrue("TaskServiceAdapter 应当是接口", TaskServiceAdapter.class.isInterface());
    }

    @Test
    public void abstractRpcTaskShouldBeAbstract() {
        assertTrue("AbstractRpcTaskServiceAdapter 应当是抽象类",
                Modifier.isAbstract(AbstractRpcTaskServiceAdapter.class.getModifiers()));
    }

    @Test
    public void abstractRpcTaskShouldImplementTaskServiceAdapter() {
        assertTrue("应当实现 TaskServiceAdapter",
                TaskServiceAdapter.class.isAssignableFrom(AbstractRpcTaskServiceAdapter.class));
    }

    @Test
    public void taskVersionShouldFormatCorrectly() throws Exception {
        Method m = AbstractRpcTaskServiceAdapter.class.getMethod("version", String.class);
        String result = (String) m.invoke(null, "todo-123");
        assertEquals("1.0.0_todo-123", result);
    }

    @Test
    public void taskVersionShouldHandleNull() throws Exception {
        Method m = AbstractRpcTaskServiceAdapter.class.getMethod("version", String.class);
        String result = (String) m.invoke(null, (String) null);
        assertEquals("1.0.0_null", result);
    }

    @Test
    public void abstractRpcTaskShouldDeclareAbstractMethods() throws NoSuchMethodException {
        String[] methods = {"sendTodoMsg", "sendDoneMsg", "sendRevokeMsg"};
        for (String name : methods) {
            boolean found = false;
            // 在抽象类的所有方法（包括继承自接口的方法）中查找
            for (Method m : AbstractRpcTaskServiceAdapter.class.getMethods()) {
                if (m.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            assertTrue("应当声明方法: " + name, found);
        }
    }
}
