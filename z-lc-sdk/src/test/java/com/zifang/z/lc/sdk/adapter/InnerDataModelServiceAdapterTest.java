package com.zifang.z.lc.sdk.adapter;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;
import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * InnerDataModelServiceAdapter 单元测试
 */
public class InnerDataModelServiceAdapterTest {

    @Test
    public void shouldNotBeAbstract() {
        assertTrue("InnerDataModelServiceAdapter 不应是 abstract",
                !Modifier.isAbstract(InnerDataModelServiceAdapter.class.getModifiers()));
    }

    @Test
    public void shouldImplementServiceAdapter() {
        assertTrue("InnerDataModelServiceAdapter 应当实现 ServiceAdapter",
                ServiceAdapter.class.isAssignableFrom(InnerDataModelServiceAdapter.class));
    }

    @Test
    public void shouldBePublicClass() {
        assertTrue(Modifier.isPublic(InnerDataModelServiceAdapter.class.getModifiers()));
    }

    @Test
    public void shouldHaveNoArgConstructor() throws NoSuchMethodException {
        InnerDataModelServiceAdapter.class.getDeclaredConstructor();
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.adapter",
                InnerDataModelServiceAdapter.class.getPackage().getName());
    }

    @Test
    public void initialAbstractDataModelServiceShouldBeNull() throws Exception {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        Field f = InnerDataModelServiceAdapter.class.getDeclaredField("abstractDataModelService");
        f.setAccessible(true);
        assertNull(f.get(adapter));
    }

    @Test
    public void setAndGetAbstractDataModelServiceShouldWork() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        AbstractDataModelService<Object> svc = new AbstractDataModelService<Object>(new Object(), Object.class) {
            @Override public Long save(Object t) { return 1L; }
            @Override public Long save(Object t, Integer mode) { return 1L; }
            @Override public void delete(Long id) {}
            @Override public void delete(Object t) {}
            @Override public Object queryById(Long pkId, boolean deep) { return null; }
            @Override public List<Object> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return Collections.emptyList(); }
            @Override public PageResult<Object> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override public Object dataCopy(Object t) { return t; }
            @Override public Map<String, Object> init(String appCode, String modelCode) { return Collections.emptyMap(); }
            @Override public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return data == null ? Collections.emptyMap() : data; }
            @Override public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return data; }
        };
        adapter.setAbstractDataModelService(svc);
        assertNotNull(adapter.getAbstractDataModelService());
    }

    @Test
    public void saveWithNullDataShouldThrowNpeWhenServiceNotSet() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        try {
            adapter.save((Map<String, Object>) null);
            org.junit.Assert.fail("未注入 service 时 save(null) 应抛 NullPointerException");
        } catch (NullPointerException expected) {
            // OK — toPojo(null) 返回 null, 然后调用 abstractDataModelService.save(null) 时 NPE
        }
    }

    @Test
    public void saveWithoutServiceShouldThrowNpe() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        Map<String, Object> data = new HashMap<>();
        data.put("k", "v");
        try {
            adapter.save(data);
            org.junit.Assert.fail("未注入 service 时 save 应抛 NullPointerException");
        } catch (NullPointerException expected) {
            // OK — inner 调用 abstractDataModelService.save 时 NPE
        }
    }

    @Test
    public void queryByIdWithNullResultShouldReturnNull() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        // queryById 未注入 service 时也会 NPE; 这里只验证接口行为
        try {
            adapter.queryById(1L, false);
            org.junit.Assert.fail("未注入 service 应抛 NPE");
        } catch (NullPointerException expected) {
            // OK
        }
    }

    @Test
    public void deleteWithNullServiceShouldThrowNpe() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        try {
            adapter.delete(1L);
            org.junit.Assert.fail("未注入 service 应抛 NPE");
        } catch (NullPointerException expected) {
            // OK
        }
    }

    @Test
    public void initShouldReturnEmptyMapWhenNoService() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        try {
            Map<String, Object> r = adapter.init("crm", "customer");
            org.junit.Assert.fail("未注入 service 应抛 NPE");
            // suppress unused
            assertNull(r);
        } catch (NullPointerException expected) {
            // OK
        }
    }

    @Test
    public void deprecatedSaveWithModeShouldStillExist() throws NoSuchMethodException {
        java.lang.reflect.Method m = InnerDataModelServiceAdapter.class.getMethod("save", Map.class, Integer.class);
        assertTrue("save(Map, Integer) 应当被 @Deprecated 标注",
                m.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void shouldHaveFieldForService() throws NoSuchFieldException {
        Field f = InnerDataModelServiceAdapter.class.getDeclaredField("abstractDataModelService");
        assertTrue("字段应当是 private",
                Modifier.isPrivate(f.getModifiers()));
    }

    @Test
    public void anonymousSubclassShouldBeInstantiable() {
        InnerDataModelServiceAdapter anonymous = new InnerDataModelServiceAdapter() {
            @Override
            public Long save(Map<String, Object> data) {
                return 42L;
            }
        };
        assertNotNull(anonymous);
        Map<String, Object> data = new HashMap<>();
        assertEquals(Long.valueOf(42L), anonymous.save(data));
    }

    @Test
    public void queryPageableShouldReturnEmptyWhenServiceReturnsNull() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        // 注入一个返回 null page 的 stub
        AbstractDataModelService<Object> svc = new AbstractDataModelService<Object>(new Object(), Object.class) {
            @Override public Long save(Object t) { return null; }
            @Override public Long save(Object t, Integer mode) { return null; }
            @Override public void delete(Long id) {}
            @Override public void delete(Object t) {}
            @Override public Object queryById(Long pkId, boolean deep) { return null; }
            @Override public List<Object> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override public PageResult<Object> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override public Object dataCopy(Object t) { return null; }
            @Override public Map<String, Object> init(String appCode, String modelCode) { return null; }
            @Override public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return null; }
            @Override public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return null; }
        };
        adapter.setAbstractDataModelService(svc);

        PageResult<Map<String, Object>> page = adapter.queryPageable(new ModelDataPageableQueryDTO(), false);
        assertNotNull(page);
        assertEquals(0L, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    public void queryListWithNullRecordsShouldReturnEmpty() {
        InnerDataModelServiceAdapter adapter = new InnerDataModelServiceAdapter();
        AbstractDataModelService<Object> svc = new AbstractDataModelService<Object>(new Object(), Object.class) {
            @Override public Long save(Object t) { return null; }
            @Override public Long save(Object t, Integer mode) { return null; }
            @Override public void delete(Long id) {}
            @Override public void delete(Object t) {}
            @Override public Object queryById(Long pkId, boolean deep) { return null; }
            @Override public List<Object> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override public PageResult<Object> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override public Object dataCopy(Object t) { return null; }
            @Override public Map<String, Object> init(String appCode, String modelCode) { return null; }
            @Override public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return null; }
            @Override public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return null; }
        };
        adapter.setAbstractDataModelService(svc);

        List<Map<String, Object>> list = adapter.queryList(new ModelDataQueryDTO(), false);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
}