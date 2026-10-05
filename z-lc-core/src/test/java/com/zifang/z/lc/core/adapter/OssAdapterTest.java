package com.zifang.z.lc.core.adapter;

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
 * OssAdapter 单元测试
 * <p>
 * OssAdapter 调用 z-oss 对象存储 API. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class OssAdapterTest {

    private OssAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new OssAdapter();
        Field f = OssAdapter.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("OssAdapter 应当标注 @Component", OssAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("OssAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(OssAdapter.class));
    }

    @Test
    public void nameShouldBeOss() {
        assertEquals("oss", adapter.name());
        assertEquals("oss", OssAdapter.NAME);
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
    public void generateDownloadUrlShouldReturnNullForNullBucket() {
        assertNull(adapter.generateDownloadUrl(null, "key", 60));
    }

    @Test
    public void generateDownloadUrlShouldReturnNullForNullKey() {
        assertNull(adapter.generateDownloadUrl("bucket", null, 60));
    }

    @Test
    public void deleteObjectShouldReturnFalseForNullBucket() {
        assertFalse(adapter.deleteObject(null, "key"));
    }

    @Test
    public void deleteObjectShouldReturnFalseForNullKey() {
        assertFalse(adapter.deleteObject("bucket", null));
    }

    // ======================================================================
    // 缺陷 #62：generateDownloadUrl / deleteObject / ping 的 HTTP 状态闸。
    // ======================================================================

    private static final String SIGN_OK_BODY =
            "{\"success\":true,\"code\":200,\"data\":\"https://oss.local/b/att.pdf?sig=abc&exp=1800\"}";

    @Test
    public void generateDownloadUrlReturnsTheUrlOnA2xxSuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body(SIGN_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertEquals("2xx 就该拿到签名 URL", "https://oss.local/b/att.pdf?sig=abc&exp=1800",
                    adapter.generateDownloadUrl("b", "att.pdf", 1800));
            assertEquals("/api/v1/object/sign-url?bucket=b&key=att.pdf&expires=1800",
                    stub.onlyRequest().path);
        }
    }

    @Test
    public void generateDownloadUrlFallsBackTo3600WhenExpiresIsNull() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body(SIGN_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertNotNull(adapter.generateDownloadUrl("b", "att.pdf", null));
            assertTrue("默认 3600 秒: " + stub.onlyRequest().path,
                    stub.onlyRequest().path.endsWith("&expires=3600"));
        }
    }

    /**
     * z-oss 回 500、body 里却带着一个 URL 时，旧写法会把这个 URL 当真签名地址发下去，
     * 前端拿到的是一个根本不存在的对象地址。
     */
    @Test
    public void generateDownloadUrlRefusesA500EvenWhenTheBodyCarriesAUrl() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(500).body(SIGN_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            String url = adapter.generateDownloadUrl("b", "att.pdf", 1800);

            assertNull("500 时不能把响应体里的 URL 当签名地址", url);
            assertEquals("确实验证过是一次真实请求", 1, stub.requestCount());
        }
    }

    @Test
    public void deleteObjectReportsSuccessOn2xx() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body("{\"success\":true,\"code\":200}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertTrue(adapter.deleteObject("b", "att.pdf"));
            assertEquals("DELETE", stub.onlyRequest().method);
            assertEquals("/api/v1/object/b?key=att.pdf", stub.onlyRequest().path);
        }
    }

    /**
     * <b>这一处的后果不是"拿到脏值"而是"撒谎"。</b>
     * 旧写法在 500 上返回 {@code true}，等于告诉调用方"这个临时文件已经清理掉了"，
     * 而它其实还好端端躺在对象存储里——流程结束时批量清理临时文件的场景下，
     * 泄漏就此发生且无人察觉。
     */
    @Test
    public void deleteObjectRefusesA500EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"message\":\"deleted\"}"))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertFalse("500 不能报删除成功——那会让调用方以为临时文件已清理", adapter.deleteObject("b", "att.pdf"));
            assertEquals("确实验证过是一次真实 DELETE", 1, stub.requestCount());
            assertEquals("DELETE", stub.onlyRequest().method);
        }
    }

    @Test
    public void deleteObjectRefusesA404EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(404)
                .body("{\"success\":true,\"code\":200,\"message\":\"deleted\"}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertFalse("404 同样不算删除成功", adapter.deleteObject("b", "missing.pdf"));
        }
    }

    @Test
    public void pingReportsUpOn2xx() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body("{\"success\":true,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertTrue(adapter.ping());
            assertEquals("/api/v1/bucket/list", stub.onlyRequest().path);
        }
    }

    @Test
    public void pingReportsDownOn500EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500).body("{\"success\":true,\"code\":200,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertFalse("z-oss 挂了不能报 UP", adapter.ping());
        }
    }

    private static void injectBaseUrl(Object target, String url) throws Exception {
        Field f = target.getClass().getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(target, url);
    }
}