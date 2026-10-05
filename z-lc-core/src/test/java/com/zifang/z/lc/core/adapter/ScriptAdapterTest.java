package com.zifang.z.lc.core.adapter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ScriptAdapter 单元测试
 * <p>
 * ScriptAdapter 调用 z-script 表达式执行 API. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class ScriptAdapterTest {

    private ScriptAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new ScriptAdapter();
        Field f = ScriptAdapter.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("ScriptAdapter 应当标注 @Component", ScriptAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("ScriptAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(ScriptAdapter.class));
    }

    @Test
    public void nameShouldBeScript() {
        assertEquals("script", adapter.name());
        assertEquals("script", ScriptAdapter.NAME);
    }

    @Test
    public void priorityShouldBeThirty() {
        assertEquals(30, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        try {
            adapter.init();
        } catch (Exception ex) {
            assertFalse("init 不应抛异常: " + ex.getMessage(), true);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void evalShouldThrowForNullScriptCode() {
        adapter.eval(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void evalShouldThrowForEmptyScriptCode() {
        adapter.eval("", null);
    }

    // ======================================================================
    // 缺陷 #62：eval 的 HTTP 状态闸。
    // ======================================================================

    @Test
    public void evalReturnsTheDataOnA2xxSuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(200)
                .body("{\"success\":true,\"code\":200,\"data\":{\"total\":42}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertNotNull("2xx 就该求值", adapter.eval("order.total", null));
            assertEquals("POST", stub.onlyRequest().method);
            assertEquals("/api/script/order.total/run", stub.onlyRequest().path);
        }
    }

    /**
     * z-script 回 500、body 里却带着一份像样的结果时，旧写法会把这个结果当求值成功返回，
     * 业务字段于是被写成一个凭空编出来的值。
     * <p>
     * 这一处的失败形态是抛异常（{@code eval} 的契约就是失败抛 {@code RuntimeException}），
     * 所以除了"确实抛了"还要盯住消息里带上了 http 状态码——否则排障时只看到一个 RuntimeException。
     */
    @Test
    public void evalRefusesA500EvenWhenTheBodyCarriesAResult() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"data\":{\"total\":999999}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            try {
                Object v = adapter.eval("order.total", null);
                assertFalse("500 不该求值成功: " + v, true);
            } catch (RuntimeException ex) {
                assertTrue("要说明是 http 500: " + ex.getMessage(), ex.getMessage().contains("500"));
            }
            assertEquals("确实验证过是一次真实请求", 1, stub.requestCount());
        }
    }

    @Test
    public void evalRefusesA404EvenWhenTheBodyCarriesAResult() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(404)
                .body("{\"success\":true,\"code\":200,\"data\":{\"total\":1}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            try {
                Object v = adapter.eval("order.total", null);
                assertFalse("404 同样不算求值成功: " + v, true);
            } catch (RuntimeException ex) {
                assertTrue("要说明是 http 404: " + ex.getMessage(), ex.getMessage().contains("404"));
            }
        }
    }

    private static void injectBaseUrl(Object target, String url) throws Exception {
        Field f = target.getClass().getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(target, url);
    }
}