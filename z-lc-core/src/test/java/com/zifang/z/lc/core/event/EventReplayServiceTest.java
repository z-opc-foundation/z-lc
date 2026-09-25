package com.zifang.z.lc.core.event;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * EventReplayService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock EventService, 注入预置事件链, 验证事件 fold (later overrides former) 规则.
 */
public class EventReplayServiceTest {

    private EventReplayService service;
    /** 按序回放的事件链 */
    private List<EventDTO> scriptedEvents;

    @Before
    public void setUp() throws Exception {
        service = new EventReplayService();
        scriptedEvents = new ArrayList<>();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("listSince".equals(name)) {
                    return new ArrayList<>(scriptedEvents);
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
                return null;
            }
        };

        EventService eventService = (EventService) Proxy.newProxyInstance(
                EventService.class.getClassLoader(),
                new Class<?>[]{EventService.class},
                handler);

        Field f = EventReplayService.class.getDeclaredField("eventService");
        f.setAccessible(true);
        f.set(service, eventService);
    }

    private EventDTO event(String eventType, String entityCode, String jsonData) {
        EventDTO ev = new EventDTO();
        ev.setEventId("evt-" + (scriptedEvents.size() + 1));
        ev.setTenantCode("t1");
        ev.setAppCode("crm");
        ev.setEntityCode(entityCode);
        ev.setEventType(eventType);
        ev.setEventData(jsonData);
        ev.setApplySeq((long) scriptedEvents.size() + 1);
        return ev;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("EventReplayService 应当标注 @Service",
                EventReplayService.class.getAnnotation(Service.class));
    }

    @Test
    public void replayShouldReturnEmptyForNoEvents() {
        List<EntityDefDTO> result = service.replay("t1", "crm");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void replayShouldFoldCreateEvent() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\",\"tableName\":\"lc_crm_order\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(1, result.size());
        EntityDefDTO def = result.get(0);
        assertEquals("order", def.getEntityCode());
        assertEquals("订单", def.getEntityName());
        assertEquals("lc_crm_order", def.getTableName());
        assertEquals("t1", def.getTenantCode());
        assertEquals("crm", def.getAppCode());
    }

    @Test
    public void replayShouldFoldCreateThenUpdate() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\"}"));
        scriptedEvents.add(event("UPDATE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单(新)\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(1, result.size());
        assertEquals("后者覆盖前者", "订单(新)", result.get(0).getEntityName());
    }

    @Test
    public void replayCreateShouldParseFields() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\",\"fields\":["
                        + "{\"fieldCode\":\"title\",\"fieldName\":\"标题\",\"fieldType\":\"STRING\",\"required\":true,\"fieldLength\":128},"
                        + "{\"fieldCode\":\"amount\",\"fieldName\":\"金额\",\"fieldType\":\"DECIMAL\",\"scale\":2}]}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(1, result.size());
        List<FieldDefDTO> fields = result.get(0).getFields();
        assertNotNull(fields);
        assertEquals(2, fields.size());
        assertEquals("title", fields.get(0).getFieldCode());
        assertEquals(Boolean.TRUE, fields.get(0).getRequired());
        assertEquals(Integer.valueOf(128), fields.get(0).getFieldLength());
        assertEquals("DECIMAL", fields.get(1).getFieldType());
        assertEquals(Integer.valueOf(2), fields.get(1).getScale());
    }

    @Test
    public void replayUpdateShouldReplaceFieldByCode() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"title\",\"fieldName\":\"标题\"}]}"));
        scriptedEvents.add(event("UPDATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"title\",\"fieldName\":\"标题(改)\"}]}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        List<FieldDefDTO> fields = result.get(0).getFields();
        assertEquals(1, fields.size());
        assertEquals("标题(改)", fields.get(0).getFieldName());
    }

    @Test
    public void replayUpdateShouldAppendNewField() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"title\",\"fieldName\":\"标题\"}]}"));
        scriptedEvents.add(event("UPDATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"amount\",\"fieldName\":\"金额\"}]}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        List<FieldDefDTO> fields = result.get(0).getFields();
        assertEquals(2, fields.size());
        assertEquals("title", fields.get(0).getFieldCode());
        assertEquals("amount", fields.get(1).getFieldCode());
    }

    @Test
    public void replayDeleteShouldRemoveField() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"title\",\"fieldName\":\"标题\"},"
                        + "{\"fieldCode\":\"amount\",\"fieldName\":\"金额\"}]}"));
        scriptedEvents.add(event("DELETE", "order",
                "{\"entityCode\":\"order\",\"fieldCode\":\"amount\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        List<FieldDefDTO> fields = result.get(0).getFields();
        assertEquals(1, fields.size());
        assertEquals("title", fields.get(0).getFieldCode());
    }

    /**
     * 缺陷 #46: 整实体的 DELETE（payload 里没有 fieldCode）过去是一个字都不做的 —— 折叠只认 fieldCode,
     * 于是管理端查不到的实体在运行时照样拼得出 SELECT、照样供旧记录。
     */
    @Test
    public void entityDeleteWithoutFieldCodeShouldDropTheWholeEntityFromTheFold() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\",\"tableName\":\"lc_crm_order\","
                        + "\"fields\":[{\"fieldCode\":\"title\"}]}"));
        scriptedEvents.add(event("UPDATE", "order", "{\"entityCode\":\"order\",\"entityName\":\"订单(改)\"}"));
        scriptedEvents.add(event("DELETE", "order", "{\"entityCode\":\"order\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertTrue("整实体 DELETE 之后折叠里不该再有这支实体: " + result, result.isEmpty());
    }

    /** 删一支折叠里从没出现过的实体, 不能反而把它"造"出来（旧写法先 computeIfAbsent 再分发）。 */
    @Test
    public void deletingAnUnknownEntityShouldNotConjureAStub() {
        scriptedEvents.add(event("DELETE", "ghost", "{\"entityCode\":\"ghost\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertTrue("一次删除动作凭空立了一支实体桩: " + result, result.isEmpty());
    }

    @Test
    public void replayShouldSkipInvalidJson() {
        scriptedEvents.add(event("CREATE", "order", "{invalid json"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertNotNull(result);
        assertTrue("无效 JSON 事件应被跳过", result.isEmpty());
    }

    @Test
    public void replayShouldSkipUnknownEventType() {
        scriptedEvents.add(event("UNKNOWN_TYPE", "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(1, result.size());
        assertEquals("未知类型只登记 entity, 不应用字段", null, result.get(0).getEntityName());
    }

    @Test
    public void replayShouldHandleNullEventType() {
        scriptedEvents.add(event(null, "order",
                "{\"entityCode\":\"order\",\"entityName\":\"订单\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(1, result.size());
    }

    @Test
    public void replayShouldAggregateMultipleEntities() {
        scriptedEvents.add(event("CREATE", "order", "{\"entityCode\":\"order\",\"entityName\":\"订单\"}"));
        scriptedEvents.add(event("CREATE", "customer", "{\"entityCode\":\"customer\",\"entityName\":\"客户\"}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        assertEquals(2, result.size());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.event",
                EventReplayService.class.getPackage().getName());
    }

    @Test
    public void fieldDefaultsShouldApplyWhenAbsent() {
        scriptedEvents.add(event("CREATE", "order",
                "{\"entityCode\":\"order\",\"fields\":[{\"fieldCode\":\"note\"}]}"));

        List<EntityDefDTO> result = service.replay("t1", "crm");

        FieldDefDTO f = result.get(0).getFields().get(0);
        assertEquals("fieldType 缺省 STRING", "STRING", f.getFieldType());
        assertEquals("required 缺省 false", Boolean.FALSE, f.getRequired());
    }
}