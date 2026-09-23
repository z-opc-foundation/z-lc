package com.zifang.z.lc.core.app;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppCreateReq;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.AppUpdateReq;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
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
 * AppAdminBizService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock LcAppEntityMapper / EntityMapper / FieldMapper / EventService,
 * 内存 Map 模拟存储, 覆盖应用状态机 (DRAFT → PUBLISHED → ARCHIVED) 与参数校验.
 */
public class AppAdminBizServiceTest {

    private AppAdminBizService service;
    private Map<Long, AppEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new AppAdminBizService();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

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
                    // 简化模拟: 返回第一条 deleted=0 的记录
                    for (AppEntity e : store.values()) {
                        if (e.getDeleted() != null && e.getDeleted() == 0) {
                            return e;
                        }
                    }
                    return null;
                }
                if ("selectList".equals(name)) {
                    return matchApps(args.length > 0 ? args[0] : null);
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
                String name = method.getName();
                if ("selectCount".equals(name)) {
                    return 0L;
                }
                if ("getLastEvent".equals(name)) {
                    return null;
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

        ClassLoader cl = AppAdminBizService.class.getClassLoader();
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
        Field f = AppAdminBizService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    /**
     * selectList 的近似实现: 默认返回全部行(含软删), 只有当 QueryWrapper 里真的出现 deleted
     * 谓词时才过滤掉软删行. 动态代理本来会彻底忽略 wrapper —— 那样"唯一性预检漏加 .eq(deleted,0)"
     * 这一类缺陷在单测层永远暴露不出来, 而它正是软删 appCode 重新创建时报 500 的根因
     * (uk_app_tenant_code 不含 deleted 列).
     */
    private List<AppEntity> matchApps(Object wrapper) {
        List<AppEntity> rows = new ArrayList<>(store.values());
        if (wrapper instanceof AbstractWrapper) {
            String seg = ((AbstractWrapper<?, ?, ?>) wrapper).getSqlSegment();
            if (seg != null && seg.contains("deleted")) {
                rows.removeIf(e -> e.getDeleted() != null && e.getDeleted() == 1);
            }
        }
        return rows;
    }

    private AppCreateReq createReq() {
        AppCreateReq req = new AppCreateReq();
        req.setAppCode("crm");
        req.setTenantCode("t1");
        req.setAppName("CRM 应用");
        return req;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("AppAdminBizService 应当标注 @Service",
                AppAdminBizService.class.getAnnotation(Service.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAppShouldRejectNullAppCode() {
        AppCreateReq req = createReq();
        req.setAppCode(null);
        service.createApp(req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAppShouldRejectNullTenantCode() {
        AppCreateReq req = createReq();
        req.setTenantCode(null);
        service.createApp(req);
    }

    @Test
    public void createAppShouldSetDraftStatus() {
        AppDTO dto = service.createApp(createReq());

        assertNotNull(dto);
        assertEquals("crm", dto.getAppCode());
        assertEquals("DRAFT", dto.getStatus());
        assertEquals(Long.valueOf(0L), dto.getCurrentVersion());
        assertEquals(Integer.valueOf(0), dto.getEntityCount());
        assertEquals(Integer.valueOf(0), dto.getFieldCount());
        assertEquals(1, store.size());
    }

    @Test
    public void createAppShouldRejectDuplicateAppCode() {
        service.createApp(createReq());

        try {
            service.createApp(createReq());
            throw new AssertionError("重复 appCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("报错应带上冲突的 appCode, 实际: " + expected.getMessage(),
                    expected.getMessage().contains("crm"));
        }
        assertEquals("冲突时不应插入新行", 1, store.size());
    }

    @Test
    public void createAppShouldRejectCodeHeldBySoftDeletedApp() {
        // 软删不会释放 app_code: uk_app_tenant_code 只包含 (tenant_code, app_code)。
        // 预检若加上 .eq("deleted",0), 这里就会放行到 insert 撞索引 -> HTTP 500 + 索引名透给前端。
        AppDTO created = service.createApp(createReq());
        service.deleteApp(created.getId());

        try {
            service.createApp(createReq());
            throw new AssertionError("被软删应用占用的 appCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("报错应说明编码被已删除应用占用, 实际: " + expected.getMessage(),
                    expected.getMessage().contains("已被删除"));
        }
    }

    @Test
    public void updateAppShouldReturnNotFoundForMissing() {
        AppUpdateReq req = new AppUpdateReq();
        req.setId(999L);
        try {
            service.updateApp(req);
            throw new AssertionError("应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void updateAppShouldMergeFields() {
        AppDTO created = service.createApp(createReq());

        AppUpdateReq update = new AppUpdateReq();
        update.setId(created.getId());
        update.setAppName("CRM (新)");
        update.setDescription("客户管理");
        update.setIcon("icon-crm");

        AppDTO updated = service.updateApp(update);

        assertEquals("CRM (新)", updated.getAppName());
        assertEquals("客户管理", updated.getDescription());
        assertEquals("icon-crm", updated.getIcon());
    }

    @Test
    public void deleteAppShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteApp(999L));
    }

    @Test
    public void deleteAppShouldSoftDelete() {
        AppDTO created = service.createApp(createReq());

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
    public void getAppByCodeShouldReturnNullForMissing() {
        assertNull(service.getAppByCode("t1", "nope"));
    }

    @Test
    public void publishAppShouldRejectNonDraft() {
        AppDTO created = service.createApp(createReq());
        store.get(created.getId()).setStatus("PUBLISHED");

        try {
            service.publishApp("t1", "crm");
            throw new AssertionError("非 DRAFT 状态应拒绝发布");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void archiveAppShouldRejectNonPublished() {
        service.createApp(createReq());

        try {
            service.archiveApp("t1", "crm");
            throw new AssertionError("非 PUBLISHED 状态应拒绝归档");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void stateMachineShouldFlowDraftToPublishedToArchived() {
        service.createApp(createReq());

        AppDTO published = service.publishApp("t1", "crm");
        assertEquals("PUBLISHED", published.getStatus());

        AppDTO archived = service.archiveApp("t1", "crm");
        assertEquals("ARCHIVED", archived.getStatus());
    }

    @Test
    public void publishAppShouldRejectMissingApp() {
        try {
            service.publishApp("t1", "nope");
            throw new AssertionError("缺失应用应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.app",
                AppAdminBizService.class.getPackage().getName());
    }
}