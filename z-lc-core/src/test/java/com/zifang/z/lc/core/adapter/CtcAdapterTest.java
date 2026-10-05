package com.zifang.z.lc.core.adapter;

import com.zifang.z.lc.common.dto.AuthContextDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * CtcAdapter 单元测试
 * <p>
 * CtcAdapter 调用 z-ctc 的鉴权 API (HTTP), 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class CtcAdapterTest {

    private CtcAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new CtcAdapter();
        Field baseUrlField = CtcAdapter.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("CtcAdapter 应当标注 @Component", CtcAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("CtcAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(CtcAdapter.class));
    }

    @Test
    public void nameShouldBeCtc() {
        assertEquals("ctc", adapter.name());
        assertEquals("ctc", CtcAdapter.NAME);
    }

    @Test
    public void priorityShouldBeTwenty() {
        assertEquals(20, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        try {
            adapter.init();
        } catch (Exception ex) {
            assertFalse("init 不应抛异常: " + ex.getMessage(), true);
        }
    }

    @Test
    public void checkAuthShouldReturnTrueForNullPermission() {
        assertTrue(adapter.checkAuth(null, null));
    }

    @Test
    public void checkAuthShouldReturnFalseForNullRequestWithPermission() {
        assertFalse(adapter.checkAuth(null, "read"));
    }

    @Test
    public void asStringShouldHandleNull() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asString", Object.class);
        m.setAccessible(true);
        assertEquals(null, m.invoke(null, (Object) null));
    }

    @Test
    public void asStringShouldConvertValue() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asString", Object.class);
        m.setAccessible(true);
        assertEquals("42", m.invoke(null, 42));
    }

    @Test
    public void asStringListShouldHandleNull() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(null, (Object) null);
        assertNotNull(result);
        assertTrue(((java.util.List<?>) result).isEmpty());
    }

    @Test
    public void asStringListShouldConvertListItems() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        java.util.List<Integer> input = new java.util.ArrayList<>();
        input.add(1);
        input.add(2);
        input.add(null);
        input.add(3);
        Object result = m.invoke(null, (Object) input);
        assertNotNull(result);
        java.util.List<?> out = (java.util.List<?>) result;
        assertEquals(3, out.size());
        assertEquals("1", out.get(0));
        assertEquals("2", out.get(1));
        assertEquals("3", out.get(2));
    }

    @Test
    public void asStringListShouldReturnEmptyForNonList() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(null, "not-a-list");
        assertNotNull(result);
        assertTrue(((java.util.List<?>) result).isEmpty());
    }

    @Test
    public void currentContextShouldReturnNullForNullRequest() {
        assertEquals(null, adapter.currentContext(null));
    }

    // ======================================================================
    // 缺陷 #62：HTTP 状态闸。判据为什么必须是 status 而不是 isSuccess()，
    // 见 HttpStatusGateTest 的类注释（那里有实测依据）。
    // 下面每一对（正/反）都必须同时在；只有反向那一支才是这条闸门的全部价值。
    // ======================================================================

    @After
    public void cleanup() {
        JwtRelayInterceptor.clear();
    }

    /** 远端给出一份正常信封时的正向证据：上下文要真的被解析出来。 */
    @Test
    public void fetchContextBuildsTheContextFromA2xxSuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(200)
                .body("{\"success\":true,\"code\":200,\"data\":{\"userId\":\"u-1\","
                        + "\"username\":\"alice\",\"tenantCode\":\"T1\",\"roles\":[\"admin\"]}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            AuthContextDTO ctx = adapter.currentContext(StubServletRequest.withAuthorization("Bearer good"));

            assertNotNull("2xx + 正常信封就该解析出上下文", ctx);
            assertEquals("u-1", ctx.getUserId());
            assertEquals("alice", ctx.getUserName());
            assertEquals("T1", ctx.getTenantCode());
            assertTrue("角色要带出来: " + ctx.getRoles(), ctx.getRoles().contains("admin"));
            assertEquals("/api/auth/verify", stub.onlyRequest().path);
        }
    }

    /**
     * <b>本族最重的一处。</b>鉴权服务 500 时，body 里若还带着一份 userId/roles，
     * 旧写法（只判 {@code isSuccess()}）会把它当成"鉴权通过"造出一个有用户身份的上下文——
     * 也就是<b>鉴权服务已经挂了，z-lc 却自己在编身份</b>。
     * <p>
     * 猎物种得很具体：<b>status=500 + body 是一份完全合法的成功信封</b>。
     * 若这条用例只回 404 + 空 body，那么"把状态闸整段删掉"也能照样绿，测不出东西。
     */
    @Test
    public void fetchContextRefusesA500EvenWhenTheBodyCarriesAUserIdentity() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"data\":{\"userId\":\"ghost-admin\","
                        + "\"username\":\"系统管理员\",\"tenantCode\":\"T0\",\"roles\":[\"admin\"]}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            AuthContextDTO ctx = adapter.currentContext(StubServletRequest.withAuthorization("Bearer ghost"));

            assertNull("鉴权服务 500 时不能造出用户上下文（否则等于凭空授予身份）", ctx);
            assertEquals("确实验证过是一次带 Authorization 的请求，不是压根没打出去",
                    1, stub.requestCount());
            assertNotNull("请求头真的要上线", stub.onlyRequest().header("authorization"));
        }
    }

    /** 404 + 一份成功信封：同一族的另一条路径（不是 500 专属）。 */
    @Test
    public void fetchContextRefusesA404EvenWhenTheBodyCarriesAUserIdentity() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(404)
                .body("{\"success\":true,\"code\":200,\"data\":{\"userId\":\"ghost-404\","
                        + "\"roles\":[\"admin\"]}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertNull("404 也不是受理", adapter.currentContext(
                    StubServletRequest.withAuthorization("Bearer ghost404")));
        }
    }

    /** 同一个 token 的 1 分钟缓存不跨适配器实例，所以每条用例各建一个 adapter 即可。 */
    @Test
    public void fetchContextCachesPerTokenWithinOneAdapter() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(200)
                .body("{\"success\":true,\"code\":200,\"data\":{\"userId\":\"u-9\",\"roles\":[]}}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertNotNull(adapter.currentContext(StubServletRequest.withAuthorization("Bearer same")));
            assertNotNull(adapter.currentContext(StubServletRequest.withAuthorization("Bearer same")));

            assertEquals("同一个 token 第二次该走缓存，不该再打一次", 1, stub.requestCount());
        }
    }

    @Test
    public void pingReportsUpOn2xx() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(200).body("{\"success\":true,\"code\":200,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertTrue("2xx 就是通", adapter.ping());
            assertEquals("/api/app/list", stub.onlyRequest().path);
        }
    }

    @Test
    public void pingReportsDownOn500EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"data\":[{\"id\":1}]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertFalse("500 不是 UP——健康检查缺陷 #52 的同一形状", adapter.ping());
        }
    }

    private static void injectBaseUrl(Object target, String url) throws Exception {
        Field f = target.getClass().getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(target, url);
    }
}