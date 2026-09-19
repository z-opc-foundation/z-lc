package com.zifang.z.lc.core.schema;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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
 * SchemaAdminBizService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock DataSource 与三个 Mapper, 覆盖 App CRUD 参数校验与唯一性约束.
 * provisionTable (DDL 执行) 路径由集成测试覆盖.
 */
public class SchemaAdminBizServiceTest {

    private SchemaAdminBizService service;
    private Map<Long, AppEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        DataSource ds = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        Class<?> rt = method.getReturnType();
                        if (rt == int.class) {
                            return 0;
                        }
                        if (rt == boolean.class) {
                            return false;
                        }
                        return null;
                    }
                });
        service = new SchemaAdminBizService(ds);

        InvocationHandler appHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    AppEntity e = (AppEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    AppEntity e = (AppEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), e);
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
                }
                if ("selectOne".equals(name)) {
                    // 简化模拟: 返回第一条 deleted=0 记录
                    for (AppEntity e : store.values()) {
                        if (e.getDeleted() != null && e.getDeleted() == 0) {
                            return e;
                        }
                    }
                    return null;
                }
                if ("selectCount".equals(name)) {
                    return 0L;
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

        InvocationHandler noopHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
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
                if ("selectCount".equals(method.getName())) {
                    return 0L;
                }
                return null;
            }
        };

        ClassLoader cl = SchemaAdminBizService.class.getClassLoader();
        LcAppEntityMapper appMapper = (LcAppEntityMapper) Proxy.newProxyInstance(cl,
                new Class<?>[]{LcAppEntityMapper.class, BaseMapper.class}, appHandler);
        EntityMapper entityMapper = (EntityMapper) Proxy.newProxyInstance(cl,
                new Class<?>[]{EntityMapper.class, BaseMapper.class}, noopHandler);
        FieldMapper fieldMapper = (FieldMapper) Proxy.newProxyInstance(cl,
                new Class<?>[]{FieldMapper.class, BaseMapper.class}, noopHandler);
        com.zifang.z.lc.core.event.EventService eventService =
                (com.zifang.z.lc.core.event.EventService) Proxy.newProxyInstance(cl,
                        new Class<?>[]{com.zifang.z.lc.core.event.EventService.class}, noopHandler);

        setField("appMapper", appMapper);
        setField("entityMapper", entityMapper);
        setField("fieldMapper", fieldMapper);
        setField("eventService", eventService);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = SchemaAdminBizService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private AppDTO appReq() {
        AppDTO req = new AppDTO();
        req.setAppCode("crm");
        req.setTenantCode("t1");
        req.setAppName("CRM");
        return req;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("SchemaAdminBizService 应当标注 @Service",
                SchemaAdminBizService.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementSchemaAdminService() {
        assertTrue("SchemaAdminBizService 应当实现 SchemaAdminService",
                SchemaAdminService.class.isAssignableFrom(SchemaAdminBizService.class));
    }

    @Test
    public void createAppShouldRejectNullAppCode() {
        AppDTO req = appReq();
        req.setAppCode(null);
        try {
            service.createApp(req);
            throw new AssertionError("null appCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void createAppShouldRejectNullTenantCode() {
        AppDTO req = appReq();
        req.setTenantCode(null);
        try {
            service.createApp(req);
            throw new AssertionError("null tenantCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void createAppShouldSetDraftDefaults() {
        AppDTO dto = service.createApp(appReq());

        assertNotNull(dto);
        assertEquals("crm", dto.getAppCode());
        assertEquals("DRAFT", dto.getStatus());
        assertEquals(Long.valueOf(0L), dto.getCurrentVersion());
        assertEquals(Integer.valueOf(0), store.get(dto.getId()).getDeleted());
    }

    @Test
    public void createAppShouldRejectDuplicate() {
        service.createApp(appReq());

        try {
            service.createApp(appReq());
            throw new AssertionError("重复 appCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void getAppShouldReturnNullForMissing() {
        assertNull(service.getApp(999L));
    }

    @Test
    public void getAppByCodeShouldReturnNullForMissing() {
        assertNull(service.getAppByCode("t1", "nope"));
    }

    @Test
    public void updateAppShouldRejectMissing() {
        try {
            service.updateApp(999L, appReq());
            throw new AssertionError("缺失应用应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void updateAppShouldIncrementCurrentVersion() {
        AppDTO created = service.createApp(appReq());

        AppDTO req = new AppDTO();
        req.setAppName("CRM (改)");
        service.updateApp(created.getId(), req);

        AppEntity stored = store.get(created.getId());
        assertEquals("CRM (改)", stored.getAppName());
        assertEquals(Long.valueOf(1L), stored.getCurrentVersion());
    }

    @Test
    public void deleteAppShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteApp(999L));
    }

    @Test
    public void deleteAppShouldSoftDelete() {
        AppDTO created = service.createApp(appReq());

        int n = service.deleteApp(created.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), store.get(created.getId()).getDeleted());
    }

    @Test
    public void listAppsShouldReturnEmptyPageResult() {
        com.zifang.util.core.meta.page.PageResult<AppDTO> result = service.listApps("t1", 1, 20);

        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertEquals(0, result.getRecords().size());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.schema",
                SchemaAdminBizService.class.getPackage().getName());
    }
}