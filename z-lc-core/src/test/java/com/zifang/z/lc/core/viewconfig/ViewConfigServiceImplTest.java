package com.zifang.z.lc.core.viewconfig;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;
import com.zifang.z.lc.core.viewconfig.entity.ViewConfigEntity;
import com.zifang.z.lc.mapper.viewconfig.ViewConfigMapper;
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
 * ViewConfigServiceImpl 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 ViewConfigMapper, 无需 Mockito / 数据库.
 */
public class ViewConfigServiceImplTest {

    private ViewConfigServiceImpl service;
    private Map<Long, ViewConfigEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new ViewConfigServiceImpl();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    ViewConfigEntity e = (ViewConfigEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    ViewConfigEntity e = (ViewConfigEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), e);
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
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

        ViewConfigMapper mapper = (ViewConfigMapper) Proxy.newProxyInstance(
                ViewConfigMapper.class.getClassLoader(),
                new Class<?>[]{ViewConfigMapper.class, BaseMapper.class},
                handler);

        Field f = ViewConfigServiceImpl.class.getDeclaredField("viewConfigMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("ViewConfigServiceImpl 应当标注 @Service",
                ViewConfigServiceImpl.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementViewConfigService() {
        assertTrue("ViewConfigServiceImpl 应当实现 ViewConfigService",
                ViewConfigService.class.isAssignableFrom(ViewConfigServiceImpl.class));
    }

    @Test
    public void createViewConfigShouldSetDefaults() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setAppCode("crm");
        req.setEntityCode("order");
        req.setViewType("TABLE");
        req.setConfig("{\"columns\":[]}");

        ViewConfigDTO dto = service.createViewConfig(req);

        assertNotNull(dto);
        assertEquals("TABLE", dto.getViewType());
        assertNotNull(dto.getCreateTime());
        assertEquals(1, store.size());
        assertEquals(Integer.valueOf(0), store.get(dto.getId()).getDeleted());
    }

    @Test
    public void updateViewConfigShouldReturnNullForMissing() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setId(999L);
        assertNull(service.updateViewConfig(req));
    }

    @Test
    public void updateViewConfigShouldMergeFields() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setViewType("TABLE");
        ViewConfigDTO created = service.createViewConfig(req);

        ViewConfigUpdateReq update = new ViewConfigUpdateReq();
        update.setId(created.getId());
        update.setViewType("FORM");
        update.setConfig("{\"layout\":\"grid\"}");

        ViewConfigDTO updated = service.updateViewConfig(update);

        assertNotNull(updated);
        assertEquals("FORM", updated.getViewType());
        assertEquals("{\"layout\":\"grid\"}", updated.getConfig());
    }

    @Test
    public void deleteViewConfigShouldSoftDelete() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        ViewConfigDTO created = service.createViewConfig(req);

        int n = service.deleteViewConfig(created.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), store.get(created.getId()).getDeleted());
    }

    @Test
    public void deleteViewConfigShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteViewConfig(999L));
    }

    @Test
    public void getViewConfigShouldReturnNullForMissing() {
        assertNull(service.getViewConfig("t1", "crm", "order", "TABLE"));
    }

    @Test
    public void listViewConfigsShouldReturnEmptyList() {
        List<ViewConfigDTO> list = service.listViewConfigs("t1", "crm", "order");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void listViewConfigsByAppShouldReturnEmptyList() {
        List<ViewConfigDTO> list = service.listViewConfigsByApp("t1", "crm");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.viewconfig",
                ViewConfigServiceImpl.class.getPackage().getName());
    }
}