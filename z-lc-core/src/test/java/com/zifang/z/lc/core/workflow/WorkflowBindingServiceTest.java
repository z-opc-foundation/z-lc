package com.zifang.z.lc.core.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowBindingMapper;
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
import static org.junit.Assert.assertSame;

/**
 * WorkflowBindingService 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 WorkflowBindingMapper, 无需 Mockito / 数据库.
 */
public class WorkflowBindingServiceTest {

    private WorkflowBindingService service;
    private Map<Long, WorkflowBindingEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new WorkflowBindingService();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    WorkflowBindingEntity e = (WorkflowBindingEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    WorkflowBindingEntity e = (WorkflowBindingEntity) args[0];
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

        WorkflowBindingMapper mapper = (WorkflowBindingMapper) Proxy.newProxyInstance(
                WorkflowBindingMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowBindingMapper.class, BaseMapper.class},
                handler);

        Field f = WorkflowBindingService.class.getDeclaredField("workflowBindingMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("WorkflowBindingService 应当标注 @Service",
                WorkflowBindingService.class.getAnnotation(Service.class));
    }

    @Test
    public void createShouldSetDefaultsAndStore() {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        e.setAppCode("crm");
        e.setEntityCode("order");
        e.setProcessDefinitionKey("expense-approval");

        WorkflowBindingEntity result = service.create(e);

        assertSame(e, result);
        assertNotNull(result.getId());
        assertEquals(Integer.valueOf(0), result.getDeleted());
        assertEquals("autoSubmit 缺省应为 1", Integer.valueOf(1), result.getAutoSubmit());
        assertNotNull(result.getCreateTime());
        assertEquals(1, store.size());
    }

    @Test
    public void createShouldKeepExplicitAutoSubmit() {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        e.setAutoSubmit(0);

        service.create(e);

        assertEquals(Integer.valueOf(0), e.getAutoSubmit());
    }

    @Test
    public void updateShouldRefreshTime() {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        service.create(e);

        WorkflowBindingEntity result = service.update(e);

        assertSame(e, result);
        assertNotNull(result.getUpdateTime());
    }

    @Test
    public void deleteShouldSoftDeleteExisting() {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
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
    public void listByAppShouldReturnEmptyListInitially() {
        List<WorkflowBindingEntity> list = service.listByApp("crm");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEntityShouldReturnEmptyListInitially() {
        List<WorkflowBindingEntity> list = service.listByEntity("crm", "order");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEventShouldReturnEmptyListInitially() {
        List<WorkflowBindingEntity> list = service.listByEvent("crm", "order", "CREATE");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.workflow",
                WorkflowBindingService.class.getPackage().getName());
    }
}