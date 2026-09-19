package com.zifang.z.lc.core.lifecycle;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import com.zifang.z.lc.sdk.spi.form.FormDataLifecycleService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ScriptLifecycleService 单元测试
 * <p>
 * 覆盖 hookScriptCode 解析与无脚本放行路径.
 * scriptExecutor (DynamicApiExecutor) 为具体类无法 JDK 代理, 脚本执行路径由集成测试覆盖.
 */
public class ScriptLifecycleServiceTest {

    private ScriptLifecycleService service;

    @Before
    public void setUp() {
        service = new ScriptLifecycleService();
    }

    private String resolve(ExtensionServiceContext ctx) throws Exception {
        Method m = ScriptLifecycleService.class.getDeclaredMethod(
                "resolveScriptCode", ExtensionServiceContext.class);
        m.setAccessible(true);
        return (String) m.invoke(service, ctx);
    }

    private ExtensionServiceContext ctx(List<String> tags) {
        ExtensionServiceContext c = new ExtensionServiceContext();
        c.setAppCode("crm");
        c.setModelCode("order");
        c.setCustomTags(tags);
        return c;
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("ScriptLifecycleService 应当标注 @Component",
                ScriptLifecycleService.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldBeAnnotatedWithInterfaceMapping() {
        InterfaceMapping mapping = ScriptLifecycleService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(mapping);
        assertEquals("ScriptLifecycleService", mapping.code());
        assertEquals("表单", mapping.group());
    }

    @Test
    public void shouldImplementFormDataLifecycleService() {
        assertTrue("应当实现 FormDataLifecycleService",
                FormDataLifecycleService.class.isAssignableFrom(ScriptLifecycleService.class));
    }

    @Test
    public void resolveScriptCodeShouldReturnNullForNullTags() throws Exception {
        assertNull(resolve(ctx(null)));
    }

    @Test
    public void resolveScriptCodeShouldReturnNullForNoMatch() throws Exception {
        assertNull(resolve(ctx(Arrays.asList("foo=bar", "baz"))));
    }

    @Test
    public void resolveScriptCodeShouldExtractValue() throws Exception {
        assertEquals("script_order_hook",
                resolve(ctx(Collections.singletonList("hookScriptCode=script_order_hook"))));
    }

    @Test
    public void resolveScriptCodeShouldReturnNullForEmptyValue() throws Exception {
        assertEquals("", resolve(ctx(Collections.singletonList("hookScriptCode="))));
    }

    @Test
    public void resolveScriptCodeShouldSkipNullTagEntry() throws Exception {
        assertNull(resolve(ctx(Arrays.asList("other", null, "another=x"))));
    }

    @Test
    public void onLifecycleShouldPassThroughWithoutHook() {
        Result<Boolean> result = service.onLifecycle(ctx(null), "beforeCreate", new HashMap<String, Object>());

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.getData());
    }

    @Test
    public void onLifecycleShouldPassThroughWithUnrelatedTags() {
        Result<Boolean> result = service.onLifecycle(
                ctx(Collections.singletonList("other=tag")), "afterCreate", null);

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.getData());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.lifecycle",
                ScriptLifecycleService.class.getPackage().getName());
    }
}