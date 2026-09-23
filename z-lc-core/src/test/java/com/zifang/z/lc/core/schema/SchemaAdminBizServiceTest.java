package com.zifang.z.lc.core.schema;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
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
    public void createAppShouldRejectCodeHeldBySoftDeletedApp() {
        // 软删不会释放 app_code: uk_app_tenant_code 只包含 (tenant_code, app_code)。
        // 预检若加上 .eq("deleted",0), 这里就会放行到 insert 撞索引 -> HTTP 500 + 索引名透给前端。
        AppDTO created = service.createApp(appReq());
        service.deleteApp(created.getId());

        try {
            service.createApp(appReq());
            throw new AssertionError("被软删应用占用的 appCode 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("报错应说明编码被已删除应用占用, 实际: " + expected.getMessage(),
                    expected.getMessage().contains("已被删除"));
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

    // ===== 缺陷 #34 / #35: 字段编码撞引擎自建列, 或者根本不是合法列名 =====

    private EntityDefDTO entityWith(String... codes) {
        EntityDefDTO def = new EntityDefDTO();
        def.setEntityCode("task");
        def.setEntityName("任务");
        def.setTableName("lc_crm_task");
        List<com.zifang.z.lc.common.dto.FieldDefDTO> fields = new ArrayList<>();
        for (String code : codes) {
            com.zifang.z.lc.common.dto.FieldDefDTO f = new com.zifang.z.lc.common.dto.FieldDefDTO();
            f.setFieldCode(code);
            f.setFieldName(code);
            f.setFieldType("STRING");
            f.setFieldLength(32);
            fields.add(f);
        }
        def.setFields(fields);
        return def;
    }

    private String ddlOf(EntityDefDTO def) throws Exception {
        Method m = SchemaAdminBizService.class.getDeclaredMethod("buildCreateTableDdl", EntityDefDTO.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(service, def);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            if (ex.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) ex.getCause();
            }
            throw ex;
        }
    }

    /** 夹具自证: 应用没建出来的话，"被拒"可能只是 App not found，测的就不是校验了。 */
    @Test
    public void fieldCodeFixtureActuallySeedsAnApp() {
        service.createApp(appReq());
        assertNotNull("夹具: app 要先在 store 里", service.getAppByCode("t1", "crm"));
        try {
            service.createEntity("t1", "crm", entityWith("title"));
        } catch (IllegalArgumentException ex) {
            throw new AssertionError("夹具: 合法编码不该被拒, 否则下面的拒绝断言是假的: " + ex.getMessage());
        }
    }

    @Test
    public void createEntityShouldRejectEveryEngineOwnedColumnCode() {
        service.createApp(appReq());
        for (String code : new String[]{"id", "tenant_code", "deleted", "create_time", "update_time"}) {
            try {
                service.createEntity("t1", "crm", entityWith(code, "title"));
                throw new AssertionError("撞引擎自建列的 fieldCode 应当在写入处就被拒: " + code);
            } catch (IllegalArgumentException expected) {
                assertTrue("报错必须点名撞名的编码, 不能只丢一句「校验失败」: " + expected.getMessage(),
                        expected.getMessage() != null && expected.getMessage().contains(code));
            }
        }
    }

    /** MySQL 列名不分大小写, `ID` 一样撞 `id`。 */
    @Test
    public void createEntityShouldRejectSystemColumnCodeIgnoringCase() {
        service.createApp(appReq());
        try {
            service.createEntity("t1", "crm", entityWith("ID"));
            throw new AssertionError("大写 ID 同样是撞引擎自建列");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ID"));
        }
    }

    /**
     * 这一组比重名更阴: 非法编码建表时被静默跳过，接口照样回"建表成功"，
     * 于是元数据说有三列、物理表一列都没有。以前只有过滤时才报 Unknown fieldCode。
     */
    @Test
    public void createEntityShouldRejectFieldCodeThatIsNotALegalColumnName() {
        service.createApp(appReq());
        for (String code : new String[]{"2bad", "我的字段", "has space", ""}) {
            try {
                service.createEntity("t1", "crm", entityWith(code));
                throw new AssertionError("非法列名应当在写入处就被拒: [" + code + "]");
            } catch (IllegalArgumentException expected) {
                assertNotNull("拒了就要给原因", expected.getMessage());
            }
        }
    }

    @Test
    public void updateEntityShouldRejectCollidingCodeBeforeAnyWrite() throws Exception {
        service.createApp(appReq());
        // 夹具: 共享的 entityMapper 是 noop, selectById 返回 null —— 直接调 updateEntity 只会拿到
        // "Entity not found", 那道闸根本没跑到。所以要先把实体喂给 selectById。
        final com.zifang.z.lc.core.executor.entity.EntityEntity row =
                new com.zifang.z.lc.core.executor.entity.EntityEntity();
        row.setId(7L);
        row.setTenantCode("t1");
        row.setAppCode("crm");
        row.setEntityCode("task");
        row.setDeleted(0);
        row.setCurrentVersion(0L);
        ClassLoader cl = SchemaAdminBizService.class.getClassLoader();
        setField("entityMapper", Proxy.newProxyInstance(cl,
                new Class<?>[]{EntityMapper.class, BaseMapper.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("selectById".equals(method.getName())) {
                            return row;
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
                        if ("selectCount".equals(method.getName())) {
                            return 0L;
                        }
                        return null;
                    }
                }));

        try {
            service.updateEntity(7L, entityWith("deleted"));
            throw new AssertionError("updateEntity 是全量替换, 同样要在写之前拦住");
        } catch (IllegalArgumentException expected) {
            // "Entity not found: 7" 不算拦住 —— 那是夹具坏了。这条要的就是原因点名。
            assertTrue("报错要点名 deleted, 否则可能只是实体不存在: " + expected.getMessage(),
                    expected.getMessage().contains("deleted"));
        }
    }

    @Test
    public void buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns() throws Exception {
        try {
            ddlOf(entityWith("title", "2bad"));
            throw new AssertionError("非法编码以前会被 `continue` 静默丢掉, DDL 照样报「成功」; 现在必须抛");
        } catch (IllegalArgumentException expected) {
            assertTrue("报错要点名被丢掉的编码: " + expected.getMessage(),
                    expected.getMessage().contains("2bad"));
        }
    }

    @Test
    public void buildCreateTableDdlShouldKeepAllLegalColumns() throws Exception {
        String ddl = ddlOf(entityWith("customer_name", "amount"));
        assertTrue("正常列要在: " + ddl, ddl.contains("`customer_name`"));
        assertTrue("正常列要在: " + ddl, ddl.contains("`amount`"));
        for (String owned : new String[]{"`id`", "`tenant_code`", "`deleted`", "`create_time`", "`update_time`"}) {
            assertTrue("引擎自建列要在 DDL 里: " + owned, ddl.contains(owned));
        }
    }
}