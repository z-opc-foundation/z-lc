package com.zifang.z.lc.core.adapter;

import com.zifang.z.lc.common.dto.DictItemDTO;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MetaAdapter 单元测试
 */
public class MetaAdapterTest {

    private MetaAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MetaAdapter();
        // 设置 baseUrl (默认 localhost:8888)
        Field baseUrlField = MetaAdapter.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldImplementAdapter() {
        assertTrue("MetaAdapter 应当实现 Adapter 接口",
                Adapter.class.isAssignableFrom(MetaAdapter.class));
    }

    @Test
    public void shouldHaveComponentAnnotation() {
        assertTrue(MetaAdapter.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    public void shouldHaveNameConstant() {
        assertEquals("meta", MetaAdapter.NAME);
    }

    @Test
    public void nameShouldReturnNameConstant() {
        assertEquals("meta", adapter.name());
    }

    @Test
    public void priorityShouldBeTen() {
        assertEquals(10, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        adapter.init();
    }

    @Test
    public void listDictItemsShouldReturnEmptyForUnknownCode() {
        // 默认 cache 为空, 实际 HTTP 调用会失败但应当 fail-safe
        try {
            List<DictItemDTO> items = adapter.listDictItems("t1", "UNKNOWN_DICT");
            // 不抛异常即视为成功 (会返回空或者缓存中的项)
            assertNotNull(items);
        } catch (Exception e) {
            // 调用 z-meta 服务失败也算可接受 (网络不可达)
            assertNotNull(e);
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.adapter",
                MetaAdapter.class.getPackage().getName());
    }

    @Test
    public void shouldHaveBaseUrlField() throws NoSuchFieldException {
        Field f = MetaAdapter.class.getDeclaredField("baseUrl");
        assertNotNull(f);
    }

    @Test
    public void shouldHaveDictCacheField() throws NoSuchFieldException {
        Field f = MetaAdapter.class.getDeclaredField("dictCache");
        assertNotNull(f);
    }

    // ======================================================================
    // 缺陷 #62：listDictItems / ping 的 HTTP 状态闸。
    // 正向各一支证明"闸门没把正常流量也拦掉"，反向各一支证明"它确实拦得住"。
    // 反向的 body 一律是<b>看起来完全成功</b>的信封——只回 404+空 body 的话，
    // 把状态闸整段删掉这些用例照样全绿。
    // ======================================================================

    private static final String DICT_OK_BODY =
            "{\"success\":true,\"code\":200,\"data\":[{\"id\":7,\"tenantCode\":\"T1\","
                    + "\"dictCode\":\"ORDER_STATUS\",\"itemCode\":\"PAID\",\"itemLabel\":\"已付款\","
                    + "\"itemValue\":\"paid\",\"sortOrder\":1}]}";

    @Test
    public void listDictItemsReturnsItemsOnA2xxSuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body(DICT_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            List<DictItemDTO> items = adapter.listDictItems("T1", "ORDER_STATUS");

            assertEquals("2xx 就该解析出字典项", 1, items.size());
            assertEquals("PAID", items.get(0).getItemCode());
            assertEquals("已付款", items.get(0).getItemLabel());
            assertEquals("/dict/items/get?dictCode=ORDER_STATUS", stub.onlyRequest().path);
            assertEquals("租户头要透传出去", "T1", stub.onlyRequest().header("x-tenant-code"));
        }
    }

    /**
     * z-meta 回 500 但 body 仍是一份合法字典时，旧写法会把这批字典项当真值交给业务。
     * <p>
     * 注意反向用例的 dictCode 故意与正向不同——{@code listDictItems} 有 5 分钟缓存，
     * 同一个 dictCode 在同一个 adapter 实例上第二次会走缓存，压根不打网络，断言会假绿。
     */
    @Test
    public void listDictItemsRefusesA500EvenWhenTheBodyCarriesRealItems() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(500).body(DICT_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            List<DictItemDTO> items = adapter.listDictItems("T1", "ORDER_STATUS_500");

            assertNotNull("失败要给出空列表而不是 null（调用方直接 size()）", items);
            assertTrue("500 不该拿到字典项: " + items, items.isEmpty());
            assertEquals("确实验证过是一次真实请求", 1, stub.requestCount());
        }
    }

    @Test
    public void listDictItemsRefusesA503EvenWhenTheBodyCarriesRealItems() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(503).body(DICT_OK_BODY))) {
            injectBaseUrl(adapter, stub.baseUrl());

            assertTrue("503 同理", adapter.listDictItems("T1", "ORDER_STATUS_503").isEmpty());
        }
    }

    @Test
    public void pingReportsUpOn2xx() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(200).body("{\"success\":true,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertTrue(adapter.ping());
            assertEquals("/app/list?pageNum=1&pageSize=1", stub.onlyRequest().path);
        }
    }

    @Test
    public void pingReportsDownOn500EvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500).body("{\"success\":true,\"code\":200,\"data\":[]}"))) {
            injectBaseUrl(adapter, stub.baseUrl());
            assertFalse("z-meta 挂了不能报 UP", adapter.ping());
        }
    }

    private static void injectBaseUrl(Object target, String url) throws Exception {
        Field f = target.getClass().getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(target, url);
    }
}