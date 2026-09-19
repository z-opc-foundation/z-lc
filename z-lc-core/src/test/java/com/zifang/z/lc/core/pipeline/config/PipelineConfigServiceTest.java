package com.zifang.z.lc.core.pipeline.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import com.zifang.z.lc.mapper.pipeline.PipelineConfigMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * PipelineConfigService 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 PipelineConfigMapper, 无需 Mockito / 数据库.
 */
public class PipelineConfigServiceTest {

    private PipelineConfigService service;
    private Map<Long, PipelineConfigEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new PipelineConfigService();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    PipelineConfigEntity e = (PipelineConfigEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    PipelineConfigEntity e = (PipelineConfigEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), e);
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
                }
                if ("deleteById".equals(name)) {
                    return store.remove((Long) args[0]) != null ? 1 : 0;
                }
                if ("selectList".equals(name) || "selectCount".equals(name)
                        || "selectOne".equals(name) || "selectMap".equals(name)) {
                    return new ArrayList<>();
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                if (List.class.isAssignableFrom(rt)) {
                    return new ArrayList<>();
                }
                if (Map.class.isAssignableFrom(rt)) {
                    return new ConcurrentHashMap<>();
                }
                return null;
            }
        };

        PipelineConfigMapper mapper = (PipelineConfigMapper) Proxy.newProxyInstance(
                PipelineConfigMapper.class.getClassLoader(),
                new Class<?>[]{PipelineConfigMapper.class, BaseMapper.class},
                handler);

        Field f = PipelineConfigService.class.getDeclaredField("pipelineConfigMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("PipelineConfigService 应当标注 @Service",
                PipelineConfigService.class.getAnnotation(Service.class));
    }

    @Test
    public void createShouldSetDefaultsAndStore() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        e.setAppCode("crm");
        e.setEntityCode("order");
        e.setTriggerEvent("CREATE");

        PipelineConfigEntity result = service.create(e);

        assertSame(e, result);
        assertNotNull(result.getId());
        assertEquals(Integer.valueOf(0), result.getDeleted());
        assertNotNull(result.getCreateTime());
        assertEquals(1, store.size());
    }

    @Test
    public void updateShouldRefreshTime() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        service.create(e);
        Long id = e.getId();

        PipelineConfigEntity result = service.update(e);

        assertSame(e, result);
        assertNotNull(result.getUpdateTime());
        assertEquals(id, result.getId());
    }

    @Test
    public void deleteShouldSoftDeleteExisting() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        service.create(e);

        int n = service.delete(e.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), e.getDeleted());
    }

    @Test
    public void deleteShouldReturnZeroForMissingId() {
        int n = service.delete(999L);
        assertEquals(0, n);
    }

    @Test
    public void toggleEnabledShouldSetOneWhenTrue() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        e.setEnabled(0);
        service.create(e);

        int n = service.toggleEnabled(e.getId(), true);

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), e.getEnabled());
    }

    @Test
    public void toggleEnabledShouldSetZeroWhenFalse() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        e.setEnabled(1);
        service.create(e);

        int n = service.toggleEnabled(e.getId(), false);

        assertEquals(1, n);
        assertEquals(Integer.valueOf(0), e.getEnabled());
    }

    @Test
    public void toggleEnabledShouldReturnZeroForMissingId() {
        int n = service.toggleEnabled(999L, true);
        assertEquals(0, n);
    }

    @Test
    public void listByAppShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByApp("crm");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEntityShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByEntity("crm", "order");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEventShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByEvent("crm", "order", "CREATE");
        assertNotNull(list);
        assertNull(list.isEmpty() ? null : list.get(0));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.pipeline.config",
                PipelineConfigService.class.getPackage().getName());
    }
}