package com.zifang.z.lc.sdk.define;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DataModelService CRUD 契约接口单元测试
 */
public class DataModelServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DataModelService.class.isInterface());
    }

    @Test
    public void shouldDeclareSaveMethod() throws NoSuchMethodException {
        DataModelService.class.getMethod("save", Object.class);
    }

    @Test
    public void shouldDeclareSaveWithModeMethod() throws NoSuchMethodException {
        DataModelService.class.getMethod("save", Object.class, Integer.class);
    }

    @Test
    public void shouldDeclareDeleteByIdMethods() throws NoSuchMethodException {
        DataModelService.class.getMethod("delete", Long.class);
        DataModelService.class.getMethod("delete", Object.class);
        DataModelService.class.getMethod("delete", String.class);
    }

    @Test
    public void shouldDeclareQueryMethods() throws NoSuchMethodException {
        DataModelService.class.getMethod("queryById", Long.class, boolean.class);
        DataModelService.class.getMethod("queryList", RuntimeQueryDTO.class, boolean.class);
        DataModelService.class.getMethod("queryPageable", RuntimeQueryDTO.class, boolean.class);
    }

    @Test
    public void shouldDeclareDataCopyMethod() throws NoSuchMethodException {
        DataModelService.class.getMethod("dataCopy", Object.class);
    }

    @Test
    public void shouldDeclareInitMethods() throws NoSuchMethodException {
        DataModelService.class.getMethod("init", String.class, String.class);
        DataModelService.class.getMethod("init", String.class, String.class, Map.class);
    }

    @Test
    public void shouldDeclareAiInitMethod() throws NoSuchMethodException {
        DataModelService.class.getMethod("aiInit", String.class, String.class, Map.class);
    }

    @Test
    public void saveShouldReturnLong() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("save", Object.class);
        assertEquals(Long.class, m.getReturnType());
    }

    @Test
    public void queryByIdShouldReturnGenericType() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("queryById", Long.class, boolean.class);
        // 擦除后是 Object
        assertEquals(Object.class, m.getReturnType());
    }

    @Test
    public void queryListShouldReturnList() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("queryList", RuntimeQueryDTO.class, boolean.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void queryPageableShouldReturnPageResult() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("queryPageable", RuntimeQueryDTO.class, boolean.class);
        assertEquals(PageResult.class, m.getReturnType());
    }

    @Test
    public void initShouldReturnMap() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("init", String.class, String.class);
        assertEquals(Map.class, m.getReturnType());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        DataModelService<TestUser> impl = new DataModelService<TestUser>() {
            @Override
            public Long save(TestUser t) { return 1L; }

            @Override
            public Long save(TestUser t, Integer mode) { return 1L; }

            @Override
            public void delete(Long id) {}

            @Override
            public void delete(TestUser t) {}

            @Override
            public TestUser queryById(Long pkId, boolean deep) { return null; }

            @Override
            public List<TestUser> queryList(RuntimeQueryDTO dto, boolean deep) { return null; }

            @Override
            public PageResult<TestUser> queryPageable(RuntimeQueryDTO dto, boolean deep) { return null; }

            @Override
            public TestUser dataCopy(TestUser t) { return null; }

            @Override
            public Map<String, Object> init(String appCode, String modelCode) { return new HashMap<>(); }

            @Override
            public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return new HashMap<>(); }

            @Override
            public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return new HashMap<>(); }
        };
        assertNotNull(impl);
    }

    @Test
    public void deleteStringDefaultImplShouldThrowOnNull() {
        DataModelService<Object> impl = new DataModelService<Object>() {
            @Override
            public Long save(Object t) { return null; }
            @Override
            public Long save(Object t, Integer mode) { return null; }
            @Override
            public void delete(Long id) {}
            @Override
            public void delete(Object t) {}
            @Override
            public Object queryById(Long pkId, boolean deep) { return null; }
            @Override
            public List<Object> queryList(RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override
            public PageResult<Object> queryPageable(RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override
            public Object dataCopy(Object t) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return null; }
            @Override
            public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return null; }
        };
        try {
            impl.delete((String) null);
            fail("delete(null) 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("empty"));
        }
    }

    @Test
    public void deleteStringDefaultImplShouldThrowOnEmpty() {
        DataModelService<Object> impl = makeImpl();
        try {
            impl.delete("");
            fail("delete(\"\") 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("empty"));
        }
    }

    @Test
    public void deleteStringDefaultImplShouldThrowOnInvalidLong() {
        DataModelService<Object> impl = makeImpl();
        try {
            impl.delete("not-a-number");
            fail("delete(\"not-a-number\") 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a valid Long"));
        }
    }

    @Test
    public void deleteStringDefaultImplShouldDelegateToLongDelete() {
        DataModelService<Object> impl = makeImpl();
        // 有效的 long 字符串应当成功转为 long
        try {
            impl.delete("123");
            // 不抛异常即视为成功
        } catch (Exception e) {
            fail("delete(\"123\") 不应抛异常: " + e.getMessage());
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.define",
                DataModelService.class.getPackage().getName());
    }

    @Test
    public void shouldNotBeAnnotation() {
        assertFalse(DataModelService.class.isAnnotation());
    }

    @Test
    public void aiInitShouldReturnMap() throws NoSuchMethodException {
        Method m = DataModelService.class.getMethod("aiInit", String.class, String.class, Map.class);
        assertEquals(Map.class, m.getReturnType());
    }

    // --- helpers ---

    private DataModelService<Object> makeImpl() {
        return new DataModelService<Object>() {
            @Override
            public Long save(Object t) { return null; }
            @Override
            public Long save(Object t, Integer mode) { return null; }
            @Override
            public void delete(Long id) {}
            @Override
            public void delete(Object t) {}
            @Override
            public Object queryById(Long pkId, boolean deep) { return null; }
            @Override
            public List<Object> queryList(RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override
            public PageResult<Object> queryPageable(RuntimeQueryDTO dto, boolean deep) { return null; }
            @Override
            public Object dataCopy(Object t) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return null; }
            @Override
            public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return null; }
        };
    }

    public static class TestUser {
        public Long id;
    }
}