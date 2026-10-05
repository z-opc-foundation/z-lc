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
 * MistAdapter 单元测试
 * <p>
 * MistAdapter 调用 z-mist 的密钥服务. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class MistAdapterTest {

    private MistAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MistAdapter();
        Field baseUrlField = MistAdapter.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("MistAdapter 应当标注 @Component", MistAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("MistAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(MistAdapter.class));
    }

    @Test
    public void nameShouldBeMist() {
        assertEquals("mist", adapter.name());
        assertEquals("mist", MistAdapter.NAME);
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

    @Test
    public void decryptShouldReturnNullForNullKey() {
        assertNull(adapter.decrypt(null, "default", "default"));
    }

    @Test
    public void decryptShouldReturnNullForEmptyKey() {
        assertNull(adapter.decrypt("", "default", "default"));
    }

    // ======================================================================
    // 缺陷 #62：decrypt / ping 的 HTTP 状态闸。
    // ======================================================================

    private static final String SECRET_OK_BODY =
            "{\"success\":true,\"code\":200,\"data\":{\"secretKey\":\"database/password\","
                    + "\"encryptedValue\":\"ENC(9f3a)\",\"decryptValue\":\"s3cr3t-plaintext\"}}";

    @Test
    public void decryptReturnsThePlaintextOnA2xxSuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body(SECRET_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertEquals("2xx 就该取到明文", "s3cr3t-plaintext",
                    adapter.decrypt("database/password", "DEFAULT_GROUP", "default"));
            assertEquals("/api/secret/database/password?group=DEFAULT_GROUP&namespace=default",
                    stub.onlyRequest().path);
        }
    }

    /**
     * <b>本族后果最直接的一处。</b>z-mist 回 500、body 里却带着 {@code decryptValue} 时，
     * 旧写法会把那一串当成<b>解密后的明文密钥</b>返回给调用方——
     * 拿一个错误响应里的字符串去做真正的加解密，后面全链路静默地错。
     * <p>
     * 猎物必须是 500 + 一份合法的密钥信封；只回 404+空 body 的话，
     * "把状态闸整段删掉"这些用例照样全绿。
     */
    @Test
    public void decryptRefusesA500EvenWhenTheBodyCarriesAPlaintextSecret() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(500).body(SECRET_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            String plain = adapter.decrypt("database/password", "DEFAULT_GROUP", "default");

            assertNull("密钥服务 500 时不能把响应体里的值当明文密钥用", plain);
            assertEquals("确实验证过是一次真实请求", 1, stub.requestCount());
        }
    }

    @Test
    public void decryptRefusesA403EvenWhenTheBodyCarriesAPlaintextSecret() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(403).body(SECRET_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertNull("403 同理", adapter.decrypt("database/password", null, null));
        }
    }

    @Test
    public void pingReportsUpOn2xx() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body("{\"success\":true,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertTrue(adapter.ping());
            assertEquals("/api/secret/list?pageNum=1&pageSize=1", stub.onlyRequest().path);
        }
    }

    @Test
    public void pingReportsDownOn500EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500).body("{\"success\":true,\"code\":200,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertFalse("z-mist 挂了不能报 UP", adapter.ping());
        }
    }

    private static void injectBaseUrl(Object target, String url) throws Exception {
        Field f = target.getClass().getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(target, url);
    }
}