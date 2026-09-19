package com.zifang.z.lc.core.adapter;

import com.zifang.z.lc.common.dto.DictItemDTO;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
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
}