package com.zifang.z.lc.core.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.core.event.entity.EventEntity;
import com.zifang.z.lc.mapper.event.EventMapper;
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
import static org.junit.Assert.assertTrue;

/**
 * EventBizService 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 EventMapper (含自定义 maxApplySeq), 无需 Mockito / 数据库.
 */
public class EventBizServiceTest {

    private EventBizService service;
    private Map<Long, EventEntity> store;
    private AtomicLong idGen;
    private AtomicLong seqGen;
    /** 模拟因果冲突开关: 让 getLastEvent 返回一个非空事件 */
    private volatile boolean hasLastEvent;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new EventBizService();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);
        seqGen = new AtomicLong(0);
        hasLastEvent = false;

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    EventEntity e = (EventEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    if (e.getApplySeq() != null && e.getApplySeq() > seqGen.get()) {
                        seqGen.set(e.getApplySeq());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("maxApplySeq".equals(name)) {
                    return seqGen.get();
                }
                if ("selectList".equals(name)) {
                    List<EventEntity> out = new ArrayList<>();
                    if (hasLastEvent && !store.isEmpty()) {
                        // 模拟 ORDER BY apply_seq DESC LIMIT 1: 返回 seq 最大的一条
                        EventEntity max = null;
                        for (EventEntity e : store.values()) {
                            if (max == null
                                    || e.getApplySeq() > max.getApplySeq()) {
                                max = e;
                            }
                        }
                        if (max != null) {
                            out.add(max);
                        }
                    }
                    return out;
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == long.class) {
                    return 0L;
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

        EventMapper mapper = (EventMapper) Proxy.newProxyInstance(
                EventMapper.class.getClassLoader(),
                new Class<?>[]{EventMapper.class, BaseMapper.class},
                handler);

        Field f = EventBizService.class.getDeclaredField("eventMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    private EventAppendRequest buildReq() {
        EventAppendRequest req = new EventAppendRequest();
        req.setTenantCode("t1");
        req.setEventType("CREATE");
        req.setEntityCode("order");
        req.setSource("USER");
        return req;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("EventBizService 应当标注 @Service",
                EventBizService.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementEventService() {
        assertTrue("EventBizService 应当实现 EventService",
                EventService.class.isAssignableFrom(EventBizService.class));
    }

    @Test
    public void appendShouldAssignMonoIncreasingSeq() {
        EventDTO first = service.append("crm", buildReq());
        EventDTO second = service.append("crm", buildReq());

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(Long.valueOf(1L), first.getApplySeq());
        assertEquals(Long.valueOf(2L), second.getApplySeq());
        assertEquals(2, store.size());
    }

    @Test
    public void appendShouldFillDefaults() {
        EventAppendRequest req = buildReq();
        req.setSource(null);

        EventDTO dto = service.append("crm", req);

        assertEquals("USER", dto.getSource());
        assertNotNull(dto.getEventId());
        assertEquals(32, dto.getEventId().length());
        assertNotNull(dto.getApplyTime());
    }

    @Test
    public void appendShouldAcceptNullParentWhenNoLastEvent() {
        hasLastEvent = false;
        EventAppendRequest req = buildReq();
        req.setParentEventId(null);

        EventDTO dto = service.append("crm", req);

        assertNotNull(dto);
    }

    @Test(expected = EventConflictException.class)
    public void appendShouldRejectWhenParentNullButLastExists() {
        service.append("crm", buildReq());
        hasLastEvent = true;

        EventAppendRequest req = buildReq();
        req.setParentEventId(null);
        service.append("crm", req);
    }

    @Test(expected = EventConflictException.class)
    public void appendShouldRejectStaleParent() {
        EventDTO first = service.append("crm", buildReq());

        EventAppendRequest req = buildReq();
        req.setParentEventId("stale-parent-id");
        service.append("crm", req);
    }

    @Test
    public void appendShouldAcceptMatchingParent() {
        EventDTO first = service.append("crm", buildReq());
        hasLastEvent = true;

        EventAppendRequest req = buildReq();
        req.setParentEventId(first.getEventId());

        EventDTO second = service.append("crm", req);

        assertNotNull(second);
        assertEquals(first.getEventId(), second.getParentEventId());
    }

    @Test
    public void getLastEventShouldReturnNullWhenEmpty() {
        assertNull(service.getLastEvent("t1", "crm"));
    }

    @Test
    public void getLastEventShouldReturnMaxSeqEvent() {
        service.append("crm", buildReq());
        EventDTO second = service.append("crm", buildReq());
        hasLastEvent = true;

        EventDTO last = service.getLastEvent("t1", "crm");

        assertNotNull(last);
        assertEquals(second.getEventId(), last.getEventId());
    }

    @Test
    public void listSinceShouldReturnEmptyList() {
        List<EventDTO> list = service.listSince("t1", "crm", 0L);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.event",
                EventBizService.class.getPackage().getName());
    }
}