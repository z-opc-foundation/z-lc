package com.zifang.z.lc.sdk.adapter;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ServiceAdapter 接口单元测试
 */
public class ServiceAdapterTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ServiceAdapter.class.isInterface());
    }

    @Test
    public void shouldDeclareSaveMethod() throws NoSuchMethodException {
        Method m = ServiceAdapter.class.getMethod("save", Map.class);
        assertEquals(Long.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeprecatedSaveWithModeMethod() throws NoSuchMethodException {
        Method m = ServiceAdapter.class.getMethod("save", Map.class, Integer.class);
        assertTrue("save(Map, Integer) 应当被 @Deprecated 标注",
                m.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void shouldDeclareDeleteMethods() throws NoSuchMethodException {
        ServiceAdapter.class.getMethod("delete", Long.class);
        ServiceAdapter.class.getMethod("delete", Map.class);
    }

    @Test
    public void shouldDeclareQueryByIdMethod() throws NoSuchMethodException {
        Method m = ServiceAdapter.class.getMethod("queryById", Long.class, boolean.class);
        assertEquals(Map.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareQueryListMethod() throws NoSuchMethodException {
        Method m = ServiceAdapter.class.getMethod("queryList", ModelDataQueryDTO.class, boolean.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareQueryPageableMethod() throws NoSuchMethodException {
        Method m = ServiceAdapter.class.getMethod("queryPageable", ModelDataPageableQueryDTO.class, boolean.class);
        assertEquals(PageResult.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareInitMethods() throws NoSuchMethodException {
        ServiceAdapter.class.getMethod("init", String.class, String.class);
        ServiceAdapter.class.getMethod("init", String.class, String.class, Map.class);
    }

    @Test
    public void shouldDeclareDataCopyMethod() throws NoSuchMethodException {
        ServiceAdapter.class.getMethod("dataCopy", Map.class);
    }

    @Test
    public void shouldDeclareAiInitMethod() throws NoSuchMethodException {
        ServiceAdapter.class.getMethod("aiInit", String.class, String.class, Map.class);
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        ServiceAdapter impl = new ServiceAdapter() {
            @Override
            public Long save(Map<String, Object> data) { return 1L; }
            @Override
            public Long save(Map<String, Object> data, Integer mode) { return 1L; }
            @Override
            public PageResult<Map<String, Object>> queryPageable(ModelDataPageableQueryDTO q, boolean deep) { return null; }
            @Override
            public void delete(Long id) {}
            @Override
            public void delete(Map<String, Object> data) {}
            @Override
            public Map<String, Object> queryById(Long id, boolean deep) { return null; }
            @Override
            public List<Map<String, Object>> queryList(ModelDataQueryDTO q, boolean deep) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode) { return null; }
            @Override
            public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) { return null; }
            @Override
            public Map<String, Object> dataCopy(Map<String, Object> data) { return null; }
            @Override
            public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) { return null; }
        };
        assertNotNull(impl);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.adapter",
                ServiceAdapter.class.getPackage().getName());
    }

    @Test
    public void shouldNotBeAnnotation() {
        assertTrue(!ServiceAdapter.class.isAnnotation());
    }
}