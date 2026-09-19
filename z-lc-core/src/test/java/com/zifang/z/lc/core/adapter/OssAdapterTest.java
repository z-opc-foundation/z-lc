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
}