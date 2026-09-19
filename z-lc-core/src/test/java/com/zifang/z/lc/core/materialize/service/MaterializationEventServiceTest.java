package com.zifang.z.lc.core.materialize.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.event.entity.EventEntity;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.mapper.event.EventMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterializationEventService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock EventMapper, 验证 MATERIALIZE 事件写入内容.
 */
public class MaterializationEventServiceTest {

    private MaterializationEventService service;
    private AtomicReference<EventEntity> lastInserted = new AtomicReference<>();

    @Before
    public void setUp() throws Exception {
        service = new MaterializationEventService();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(name(method))) {
                    lastInserted.set((EventEntity) args[0]);
                    return 1;
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                if (List.class.isAssignableFrom(rt)) {
                    return new java.util.ArrayList<>();
                }
                if (Map.class.isAssignableFrom(rt)) {
                    return new ConcurrentHashMap<>();
                }
                return null;
            }

            private String name(Method m) {
                return m.getName();
            }
        };

        EventMapper mapper = (EventMapper) Proxy.newProxyInstance(
                EventMapper.class.getClassLoader(),
                new Class<?>[]{EventMapper.class, BaseMapper.class},
                handler);

        Field f = MaterializationEventService.class.getDeclaredField("eventMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    private MaterializationEntity entity() {
        MaterializationEntity m = new MaterializationEntity();
        m.setId(7L);
        m.setTenantCode("t1");
        m.setAppCode("crm");
        m.setExportVersion("20260919-001");
        m.setMaterializationPath("/tmp/export");
        m.setTriggerSource("USER");
        return m;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("MaterializationEventService 应当标注 @Service",
                MaterializationEventService.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldInsertEventAndReturnId() {
        String eventId = service.recordMaterializeEvent(entity(), 6);

        assertNotNull(eventId);
        assertNotNull("insert 应被调用", lastInserted.get());
        assertEquals(32, eventId.length());
    }

    @Test
    public void shouldWriteMaterializeEventTypeAndSource() {
        service.recordMaterializeEvent(entity(), 6);

        EventEntity e = lastInserted.get();
        assertEquals(MaterializationEventService.EVENT_TYPE_MATERIALIZE, e.getEventType());
        assertEquals(MaterializationEventService.SOURCE_GENERATOR, e.getSource());
        assertEquals("crm", e.getAppCode());
        assertEquals("t1", e.getTenantCode());
        assertNull("MATERIALIZE 作用于多个 entity, entityCode 应为 null", e.getEntityCode());
    }

    @Test
    public void shouldWriteJsonDataWithKeyFields() {
        service.recordMaterializeEvent(entity(), 6);

        String data = lastInserted.get().getEventData();
        assertNotNull(data);
        assertTrue("应包含 materialization_id", data.contains("\"materialization_id\":7"));
        assertTrue("应包含 export_version", data.contains("20260919-001"));
        assertTrue("应包含 file_count", data.contains("\"file_count\":6"));
        assertTrue("应包含 trigger_source", data.contains("USER"));
    }

    @Test
    public void shouldApplySeqBeNonNull() {
        service.recordMaterializeEvent(entity(), 6);

        assertNotNull(lastInserted.get().getApplySeq());
        assertNotNull(lastInserted.get().getApplyTime());
    }

    @Test
    public void constantsShouldBeStable() {
        assertEquals("MATERIALIZE", MaterializationEventService.EVENT_TYPE_MATERIALIZE);
        assertEquals("GENERATOR", MaterializationEventService.SOURCE_GENERATOR);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.materialize.service",
                MaterializationEventService.class.getPackage().getName());
    }
}