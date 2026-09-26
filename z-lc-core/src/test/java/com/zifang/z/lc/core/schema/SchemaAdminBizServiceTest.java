package com.zifang.z.lc.core.schema;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * SchemaAdminBizService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock DataSource 与三个 Mapper, 覆盖 App CRUD 参数校验与唯一性约束.
 * provisionTable 的 DDL 执行路径由 {@link FakeJdbc} 顶住 (缺陷 #51 起): 那条路上每一步都要问数据库，
 * 用真库测不出"问的是哪张表"，用 null 连接又只会一路走到"读不到"的分支。
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

    // ===== 缺陷 #47: 定义跑在物理表前面之后，provision 要能把缺的那几列补上 =====
    //
    // 补列是**有副作用**的 DDL，所以这一族要钉的是三件事:
    //  ① 补出来的列必须和建表建出来的是同一份定义（宽度/可空/默认值/注释），两处各写一份类型映射
    //     迟早一个改了一个没改，而这种不一致只有等下一次数据写歪了才看得见;
    //  ② 只 ADD，不动已有列（DROP/MODIFY/CHANGE 一个都不许出现在这句话里）;
    //  ③ 只补**自己那张表** —— 两张定义共用一张物理表时补列等于拿 B 的定义去改 A 的表。
    //
    // 由 _e2e/mutate_provision_reconcile_guard.py 逐支注入反证（改 columnClause / buildAddColumnDdl /
    // otherLiveEntityOnSameTable 的那几个谓词）。

    private String addColumnDdlOf(String tableName, com.zifang.z.lc.common.dto.FieldDefDTO f) throws Exception {
        Method m = SchemaAdminBizService.class.getDeclaredMethod(
                "buildAddColumnDdl", String.class, com.zifang.z.lc.common.dto.FieldDefDTO.class);
        m.setAccessible(true);
        return (String) m.invoke(service, tableName, f);
    }

    private com.zifang.z.lc.core.executor.entity.EntityEntity squatterOf(EntityDefDTO def) throws Exception {
        Method m = SchemaAdminBizService.class.getDeclaredMethod(
                "otherLiveEntityOnSameTable", EntityDefDTO.class);
        m.setAccessible(true);
        return (com.zifang.z.lc.core.executor.entity.EntityEntity) m.invoke(service, def);
    }

    /**
     * 把 entityMapper 换成"返回这批行、并且**真的按 wrapper 里的谓词过滤**"的替身。
     * <p>
     * 默认的 noop 替身会把 QueryWrapper 整个忽略 —— 那样"漏加 .eq(deleted, 0)"或"漏加租户"
     * 在单测层永远暴露不出来（和上面 {@link #matchApps} 同一个理由）。
     */
    private void useEntityRows(final com.zifang.z.lc.core.executor.entity.EntityEntity... rows) throws Exception {
        final List<com.zifang.z.lc.core.executor.entity.EntityEntity> pool =
                new ArrayList<>(java.util.Arrays.asList(rows));
        ClassLoader cl = SchemaAdminBizService.class.getClassLoader();
        setField("entityMapper", Proxy.newProxyInstance(cl,
                new Class<?>[]{EntityMapper.class, BaseMapper.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("selectList".equals(method.getName())) {
                            return matchEntities(pool, args.length > 0 ? args[0] : null);
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
    }

    private com.zifang.z.lc.core.executor.entity.EntityEntity entityRow(long id, String tenant, String table,
                                                                        int deleted, String entityCode) {
        com.zifang.z.lc.core.executor.entity.EntityEntity e =
                new com.zifang.z.lc.core.executor.entity.EntityEntity();
        e.setId(id);
        e.setTenantCode(tenant);
        e.setAppCode("crm");
        e.setEntityCode(entityCode);
        e.setTableName(table);
        e.setDeleted(deleted);
        return e;
    }

    /** wrapper 里 <code>column = #{ew.paramNameValuePairs.MPGENVALn}</code> 绑的那个值；谓词不存在返回 null。 */
    private static Object predicateValue(Object wrapper, String column) {
        if (!(wrapper instanceof AbstractWrapper)) {
            return null;
        }
        AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(column + "\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}")
                .matcher(String.valueOf(aw.getSqlSegment()));
        if (!m.find()) {
            return null;
        }
        return aw.getParamNameValuePairs().get(m.group(1));
    }

    private List<com.zifang.z.lc.core.executor.entity.EntityEntity> matchEntities(
            List<com.zifang.z.lc.core.executor.entity.EntityEntity> rows, Object wrapper) {
        List<com.zifang.z.lc.core.executor.entity.EntityEntity> kept = new ArrayList<>(rows);
        Object deleted = predicateValue(wrapper, "deleted");
        if (deleted != null) {
            kept.removeIf(e -> !String.valueOf(deleted).equals(String.valueOf(e.getDeleted())));
        }
        Object tenant = predicateValue(wrapper, "tenant_code");
        if (tenant != null) {
            kept.removeIf(e -> !String.valueOf(tenant).equals(e.getTenantCode()));
        }
        return kept;
    }

    /** 夹具自证: 替身真的按谓词过滤，否则下面"没报占着"那几条都是假的。 */
    @Test
    public void entityRowFixtureReallyAppliesTheWrapperPredicates() throws Exception {
        useEntityRows(entityRow(2L, "t1", "lc_crm_task", 0, "other"),
                entityRow(3L, "t1", "lc_crm_ghost", 1, "ghost"),
                entityRow(4L, "t2", "lc_crm_other_tenant", 0, "elsewhere"));
        // 谓词若真的生效: deleted=0 那支在 t1 命中, 软删/跨租户的两支都进不来。
        EntityDefDTO inT1 = defOnTable("t1", 1L, "lc_crm_task");
        assertNotNull("夹具: 未删除同租户的占表行要报出来", squatterOf(inT1));
        assertNull("夹具: 谓词没生效(替身把 wrapper 忽略了), 下面的账都不能信",
                squatterOf(defOnTable("t1", 1L, "lc_crm_ghost")));
        assertNull("夹具: 谓词没生效(租户过滤漏了), 下面的账都不能信",
                squatterOf(defOnTable("t9", 1L, "lc_crm_other_tenant")));
    }

    private EntityDefDTO defOnTable(String tenant, Long id, String table) {
        EntityDefDTO def = entityWith("title", "amount");
        def.setTenantCode(tenant);
        def.setId(id);
        def.setTableName(table);
        return def;
    }

    @Test
    public void addColumnShouldUseTheSameClauseAsCreateTable() throws Exception {
        EntityDefDTO def = entityWith("amount");
        com.zifang.z.lc.common.dto.FieldDefDTO f = def.getFields().get(0);
        f.setFieldType("DECIMAL");
        f.setFieldLength(18);
        f.setScale(2);
        f.setRequired(true);
        f.setDefaultValue("0.00");
        f.setDescription("金额");

        String alter = addColumnDdlOf(def.getTableName(), f);
        String added = alter.substring(alter.indexOf("ADD COLUMN") + "ADD COLUMN".length()).trim();
        String created = null;
        for (String line : ddlOf(def).split("\n")) {
            String t = line.trim();
            if (t.startsWith("`amount`")) {
                created = t.endsWith(",") ? t.substring(0, t.length() - 1) : t;
            }
        }
        assertNotNull("夹具: 建表语句里要真有 amount 那一行", created);
        // 这一条把两处绑成同一个来源: 谁单独改了类型映射、可空性、默认值或注释, 这里就红。
        assertEquals("补出来的列必须和建表建出来的那一列逐字相同: ", created, added);
        assertTrue("两边都得带上 NOT NULL: " + added, added.contains("NOT NULL"));
    }

    @Test
    public void addColumnShouldOnlyAddAndNeverRewriteOrDrop() throws Exception {
        EntityDefDTO def = entityWith("amount");
        String alter = addColumnDdlOf(def.getTableName(), def.getFields().get(0));
        assertTrue("必须是 ADD COLUMN: " + alter, alter.contains("ADD COLUMN"));
        for (String forbidden : new String[]{"DROP", "MODIFY", "CHANGE COLUMN", "TRUNCATE", "UPDATE "}) {
            assertTrue("补列不许动已有列/数据, 但语句里有 " + forbidden + ": " + alter,
                    !alter.toUpperCase().contains(forbidden));
        }
    }

    @Test
    public void reconcileShouldRefuseTableHeldByAnotherLiveEntity() throws Exception {
        useEntityRows(entityRow(2L, "t1", "lc_crm_task", 0, "invoice"));
        com.zifang.z.lc.core.executor.entity.EntityEntity held =
                squatterOf(defOnTable("t1", 1L, "lc_crm_task"));
        assertNotNull("别的未删除实体占着这张表, 补列之前必须问出来", held);
        // 报错文案要能指到是谁占着: 只回一个"表被占用"没人查得动。
        assertEquals("invoice", held.getEntityCode());
        assertEquals("crm", held.getAppCode());
    }

    /** 自己那一行不算占着 —— 否则每次 provision 都会把自己判死。 */
    @Test
    public void ownRowShouldNotCountAsSquatter() throws Exception {
        useEntityRows(entityRow(1L, "t1", "lc_crm_task", 0, "task"));
        assertNull("自己占自己的表不是抢占", squatterOf(defOnTable("t1", 1L, "lc_crm_task")));
    }

    /** 软删实体留下的墓碑不算占着: 删实体不删表，算进去等于一个表名被永久占死。 */
    @Test
    public void softDeletedEntityShouldNotBlockReconcile() throws Exception {
        useEntityRows(entityRow(2L, "t1", "lc_crm_task", 1, "old_task"));
        assertNull("软删的那支不该拦住补列", squatterOf(defOnTable("t1", 1L, "lc_crm_task")));
    }

    /** 租户隔离: 别的租户指向同一个表名不是抢占（表在各自的数据源命名空间里）。 */
    @Test
    public void otherTenantShouldNotBlockReconcile() throws Exception {
        useEntityRows(entityRow(2L, "t2", "lc_crm_task", 0, "other_tenant_task"));
        assertNull("跨租户不该算抢占", squatterOf(defOnTable("t1", 1L, "lc_crm_task")));
    }

    /** MySQL/H2 的表名不分大小写: `TBL` 和 `tbl` 抢的是同一张表。 */
    @Test
    public void squatterMatchShouldIgnoreTableNameCase() throws Exception {
        useEntityRows(entityRow(2L, "t1", "LC_CRM_TASK", 0, "invoice"));
        assertNotNull("大小写不同的同一张表也要报占着", squatterOf(defOnTable("t1", 1L, "lc_crm_task")));
    }

    // ===== 缺陷 #51: 运行时表和元数据层差一个校对级别，列表页当场 500 =====
    //
    // 250 上真 MySQL 8 实测 (information_schema.tables): 元数据层 15 张 z_lc_* 全是 utf8mb4_general_ci
    // (沿用库默认)，而引擎自己 buildCreateTableDdl 出来的 e2e_customer_404937 是 utf8mb4_0900_ai_ci
    // (只写了 charset，MySQL 8 取该 charset 的默认校对)。一轮 API 门禁 28 条红里 8 条是
    // Illegal mix of collations。所以这一组要钉住三件事: 新表钉上元数据层那一套、问不到时不许编一个
    // 校对、已经错的表要判红并给出可执行的修法。

    /**
     * 顶住 provisionOne 里所有会走到数据库的三步: 执行 DDL、问元数据表的 {字符集, 校对}、问物理表的校对。
     * <p>
     * SQL 一律记账: "问到了"和"随便问了一条结果被吞了"在看测试的人眼里是一样的，除非这里能指出
     * 问的是哪张表哪一栏。
     */
    private static final class FakeJdbc extends JdbcTemplate {
        final List<String> executed = new ArrayList<String>();
        Map<String, Object> metaRow;
        String tableCollation;
        List<String> physicalColumns = new ArrayList<String>();
        /** 表里有没有行 (null = 探不到，等价于"库问不答应")。 */
        Boolean hasRows = Boolean.FALSE;
        /** true = 发出去的 ADD COLUMN 真的进物理列清单（库答应了），用来量"回读"那一头。 */
        boolean appliesAddColumn;

        FakeJdbc(DataSource ds) {
            super(ds);
        }

        @Override
        public void execute(String sql) {
            executed.add(sql);
            if (appliesAddColumn && sql.contains("ADD COLUMN `")) {
                String code = sql.substring(sql.indexOf("ADD COLUMN `") + 12);
                physicalColumns.add(code.substring(0, code.indexOf('`')).toLowerCase(java.util.Locale.ROOT));
            }
        }

        /**
         * 行数探针走的是 {@code queryForList(String)} 这一条**非**可变参的重载 ——
         * 和上面 queryForMap 同一个坑：不替这一条，生产代码会拿真实现去问替身 DataSource 的连接。
         */
        @Override
        public List<Map<String, Object>> queryForList(String sql) {
            executed.add(sql);
            if (hasRows == null) {
                throw new IllegalStateException("cannot tell whether the table has rows");
            }
            List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
            if (hasRows.booleanValue()) {
                Map<String, Object> one = new HashMap<String, Object>();
                one.put("1", Integer.valueOf(1));
                out.add(one);
            }
            return out;
        }

        @Override
        public Map<String, Object> queryForMap(String sql) {
            return ask(sql);
        }

        @Override
        public Map<String, Object> queryForMap(String sql, Object... args) {
            return ask(sql);
        }

        /**
         * JdbcTemplate 同时有 {@code queryForMap(String)} 和 {@code queryForMap(String, Object...)}，
         * 单参调用绑到前者 —— 只替一条的话另一条会悄悄走真实现，然后从替身 DataSource 那里拿到
         * null 连接、被生产的 catch 吞成"问不到"，测试就以"退回旧写法"的名义全绿。两条都替。
         */
        private Map<String, Object> ask(String sql) {
            executed.add(sql);
            if (metaRow == null) {
                // 等价于 dev 的 H2: 没有 information_schema，问不到就是问不到。
                throw new IllegalStateException("no information_schema here");
            }
            return metaRow;
        }

        /**
         * 表级校对那一问按 <b>MySQL 8 实测的目录形状</b>答话: tables 视图里表级那一列叫
         * {@code TABLE_COLLATION}, 拿 columns 的 {@code COLLATION_NAME} 去问会当场
         * {@code ERROR 1054 Unknown column}（250 上量的）。替身原先不看 SQL、照编排返回值 ——
         * 于是缺陷 #57（那句 SQL 写错列名，被产品的 catch 吞成"问不到"，整道校对闸一次都不咬）
         * 在单测层结构上打不到，注了也不红，只能记一笔"未覆盖"。现在问错就抛，等价于真库拒绝，
         * 产品那条 fail-open 路径当场露出来。<b>这不是证明 MySQL 长这样</b>——那一半由
         * {@code _e2e/deploy_250.sh gate4} 在真库上证；这里只让"问错列名"不再是隐形缺陷。
         */
        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            executed.add(sql);
            String s = sql.toLowerCase(java.util.Locale.ROOT);
            if (s.contains("information_schema.tables")
                    && (!s.contains("table_collation") || s.contains("collation_name"))) {
                throw new IllegalStateException("Unknown column 'collation_name' in 'field list'");
            }
            if (tableCollation == null) {
                throw new IllegalStateException("table not found");
            }
            return requiredType.cast(tableCollation);
        }

        /** 元数据层用的那一套 (charset, collation)。 */
        void metadataIs(String charset, String collation) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("character_set_name", charset);
            row.put("collation_name", collation);
            metaRow = row;
        }

        int asksToInformationSchema() {
            int n = 0;
            for (String sql : executed) {
                if (sql.contains("information_schema")) {
                    n++;
                }
            }
            return n;
        }
    }

    /** 装上 FakeJdbc (连带一张可编排的物理列清单)，并返回它以便逐条编排读数。 */
    private FakeJdbc useFakeJdbc(final List<String> physicalColumns) throws Exception {
        Object conn = Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getMetaData".equals(method.getName())) {
                            return dbMeta(physicalColumns);
                        }
                        return jdbcDefault(method);
                    }
                });
        DataSource ds = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getConnection".equals(method.getName())) {
                            return conn;
                        }
                        return jdbcDefault(method);
                    }
                });
        FakeJdbc jdbc = new FakeJdbc(ds);
        jdbc.physicalColumns = physicalColumns;
        setField("jdbcTemplate", jdbc);
        return jdbc;
    }

    private static Object dbMeta(final List<String> columns) {
        return Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(), new Class<?>[]{DatabaseMetaData.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getColumns".equals(method.getName())) {
                            return columnsRs(columns);
                        }
                        return jdbcDefault(method);
                    }
                });
    }

    private static Object columnsRs(final List<String> columns) {
        final int[] cursor = {-1};
        return Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("next".equals(name)) {
                            cursor[0]++;
                            return Boolean.valueOf(cursor[0] < columns.size());
                        }
                        if ("getString".equals(name)) {
                            return columns.get(cursor[0]);
                        }
                        return jdbcDefault(method);
                    }
                });
    }

    private static Object jdbcDefault(Method method) {
        Class<?> rt = method.getReturnType();
        if (rt == boolean.class) {
            return Boolean.FALSE;
        }
        if (rt == int.class) {
            return Integer.valueOf(0);
        }
        if (rt == long.class) {
            return Long.valueOf(0L);
        }
        return null;
    }

    private String tableCharsetClauseOf() throws Exception {
        Method m = SchemaAdminBizService.class.getDeclaredMethod("tableCharsetClause");
        m.setAccessible(true);
        return (String) m.invoke(service);
    }

    private ProvisionReport.Item provisionOneOf(EntityDefDTO def) throws Exception {
        Method m = SchemaAdminBizService.class.getDeclaredMethod("provisionOne", EntityDefDTO.class);
        m.setAccessible(true);
        return (ProvisionReport.Item) m.invoke(service, def);
    }

    /** 一张"列一列不缺"的表: 让 provisionOne 一路走到校对那道闸，而不是半路红在别处。 */
    private static List<String> intactColumnsOf(EntityDefDTO def) {
        List<String> cols = new ArrayList<String>(SchemaAdminBizService.SYSTEM_COLUMN_CODES);
        for (com.zifang.z.lc.common.dto.FieldDefDTO f : def.getFields()) {
            cols.add(f.getFieldCode().toLowerCase(java.util.Locale.ROOT));
        }
        return cols;
    }

    /** 修好之后新表要沿用元数据层那一套，而不是各自取 charset 默认。 */
    @Test
    public void createTableDdlShouldPinTheMetadataLayerCollation() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));
        jdbc.metadataIs("utf8mb4", "utf8mb4_general_ci");

        String ddl = ddlOf(def);
        assertTrue("建表语句必须钉上元数据层那套校对, 否则 MySQL 8 会静默取 utf8mb4_0900_ai_ci: " + ddl,
                ddl.contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"));
        // 夹具自证: 上面那句绿了不等于问对了地方 —— 得看它问的是元数据表那一栏。
        assertTrue("要去问元数据表的实际校对, 不能写死一个: " + jdbc.executed,
                jdbc.executed.toString().contains("z_lc_dict_item")
                        && jdbc.executed.toString().contains("item_code"));
    }

    /**
     * 问不到 (dev 的 H2、没有 information_schema 权限、库还没建元数据表) 时退回修复前的写法。
     * <p>
     * 这一条是上一条的负控: 如果"编一个校对出来"也能让上一条绿，那这一条会红。
     */
    @Test
    public void createTableDdlShouldNotInventACollationWhenMetadataIsUnaskable() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));
        // metaRow 留 null = 问不到

        String ddl = ddlOf(def);
        assertTrue("问不到也要带上原来的 charset, 与修复前一致: " + ddl, ddl.contains("DEFAULT CHARSET=utf8mb4"));
        assertTrue("问不到就不许凭猜测写一个 COLLATE: " + ddl, !ddl.contains("COLLATE"));
    }

    /** 校对读数不是标识符就不许进 DDL —— 那是一段由数据库内容拼出来的 SQL 尾巴。 */
    @Test
    public void nonIdentifierCollationReadingMustNotReachDdl() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));
        jdbc.metadataIs("utf8mb4", "general_ci; DROP TABLE z_lc_app --");

        String clause = tableCharsetClauseOf();
        assertTrue("坏读数一律当问不到处理: " + clause, clause.equals("DEFAULT CHARSET=utf8mb4"));
        assertTrue("更不能把那句 SQL 抄进建表语句: " + clause, !clause.contains("DROP"));
    }

    /**
     * 问不到不能缓存成"永远问不到"：元数据表可能就在下一次 provision 之后才出现。
     * 反过来，问到了只问一次，别为每一次建表都多打一条 information_schema 查询。
     */
    @Test
    public void metadataCollationShouldBeReaskedUntilItResolves() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));

        tableCharsetClauseOf();
        tableCharsetClauseOf();
        int asksWhileUnresolvable = jdbc.asksToInformationSchema();
        assertTrue("问不到时每次都要重新问 (缓存一个失败就等于把这道闸永久关掉): " + asksWhileUnresolvable,
                asksWhileUnresolvable >= 2);

        jdbc.metadataIs("utf8mb4", "utf8mb4_general_ci");
        tableCharsetClauseOf();
        tableCharsetClauseOf();
        assertEquals("问到了只问一次: ", asksWhileUnresolvable + 1, jdbc.asksToInformationSchema());
    }

    @Test
    public void collationRepairMessageShouldNameTheFixOnlyOnARealMismatch() throws Exception {
        String bad = SchemaAdminBizService.collationRepairMessage(
                "lc_crm_task", "utf8mb4", "utf8mb4_general_ci", "utf8mb4_0900_ai_ci");
        assertNotNull("两边差一个校对级别就是 250 上那 8 条红的形状, 必须判", bad);
        assertTrue("要给出可直接执行的修法: " + bad,
                bad.contains("ALTER TABLE `lc_crm_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"));
        assertTrue("要点名错在哪一边: " + bad,
                bad.contains("utf8mb4_0900_ai_ci") && bad.contains("utf8mb4_general_ci"));

        assertNull("一致就不判: " + bad, SchemaAdminBizService.collationRepairMessage(
                "lc_crm_task", "utf8mb4", "utf8mb4_general_ci", "utf8mb4_general_ci"));
        assertNull("表那一侧问不到 (H2) 不判", SchemaAdminBizService.collationRepairMessage(
                "lc_crm_task", "utf8mb4", "utf8mb4_general_ci", null));
        assertNull("元数据那一侧问不到不判", SchemaAdminBizService.collationRepairMessage(
                "lc_crm_task", "utf8mb4", null, "utf8mb4_0900_ai_ci"));
    }

    /**
     * 这一条才是"闸真的在 provisionOne 的路上"。上面几条测的都是零件；零件全绿而装配顺序把这段
     * 挪到 return 之后，产品照样一句都不红。
     */
    @Test
    public void provisionShouldFailWhenTableCollationDiffersFromMetadataLayer() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));
        jdbc.metadataIs("utf8mb4", "utf8mb4_general_ci");
        jdbc.tableCollation = "utf8mb4_0900_ai_ci";

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals("列一列不缺但读不出来的表, 不能报成建好了: " + item.getMessage(),
                ProvisionReport.FAILED, item.getStatus());
        // 整句而不是"含不含 CONVERT": 只判有没有这个短语的话, 把 charset 和 collation 两个参数换序
        // 也照样绿，而那时候界面上给出的修法跑下去会把表改成另一个校对。
        assertTrue("报错要带着可执行且指向正确校对的修法: " + item.getMessage(),
                item.getMessage().contains(
                        "ALTER TABLE `lc_crm_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"));
    }

    /** 同一个装配位: 校对一致、或压根问不到时，不许凭空多一道红。 */
    @Test
    public void provisionShouldStayGreenWhenCollationMatchesOrIsUnaskable() throws Exception {
        EntityDefDTO def = entityWith("title");
        FakeJdbc jdbc = useFakeJdbc(intactColumnsOf(def));
        jdbc.metadataIs("utf8mb4", "utf8mb4_general_ci");
        jdbc.tableCollation = "utf8mb4_general_ci";
        ProvisionReport.Item ok = provisionOneOf(def);
        assertTrue("一致时不该判红: " + ok.getMessage(), !ProvisionReport.FAILED.equals(ok.getStatus()));

        FakeJdbc h2 = useFakeJdbc(intactColumnsOf(def));
        h2.metadataIs("utf8mb4", "utf8mb4_general_ci");
        // tableCollation 留 null = H2 里没有 information_schema.tables 这一问
        ProvisionReport.Item lenient = provisionOneOf(def);
        assertTrue("问不到就不判 (否则 dev 环境每一次 provision 都红): " + lenient.getMessage(),
                !ProvisionReport.FAILED.equals(lenient.getStatus()));
    }

    // ===== 缺陷 #54: 给「有行的表」补「必填且无默认值」的列，结局由库决定 =====
    //
    // 250 上真 MySQL 8 打出来的第 3 个只有部署才会撞的坑: 同一条 ADD COLUMN，dev 的 H2 当场拒
    // ("NULL not allowed")，而 mysql:8.0.26 即便 @@sql_mode 含 STRICT_TRANS_TABLES 也**接受**，
    // 并把已有行的那一栏静默填成空串 —— provision 于是报 ALTERED「成功」，库里却已经躺着一批
    // 违反「这一栏必填」的行。这一族钉的是: 分叉的前提是「有没有行」，不是「必不必填」。
    // 由 _e2e/mutate_collation_guard.py 的 N1–N4 逐支注入反证。

    private EntityDefDTO defWithRequired(boolean withDefault) {
        return defWithDeclaredDefault(withDefault ? "未填" : null);
    }

    private EntityDefDTO defWithDeclaredDefault(String defaultValue) {
        EntityDefDTO def = entityWith("seed");
        com.zifang.z.lc.common.dto.FieldDefDTO f = new com.zifang.z.lc.common.dto.FieldDefDTO();
        f.setFieldCode("needs_value");
        f.setFieldName("必填列");
        f.setFieldType("STRING");
        f.setFieldLength(32);
        f.setRequired(true);
        f.setDefaultValue(defaultValue);
        def.getFields().add(f);
        return def;
    }

    /** 一张「就缺 needs_value 这一栏」的表 —— 让 provisionOne 真的走进补列那一段。 */
    private static List<String> colsMissingOne(EntityDefDTO def, String absent) {
        List<String> cols = intactColumnsOf(def);
        cols.remove(absent);
        return cols;
    }

    private static List<String> executedAdds(FakeJdbc jdbc) {
        List<String> out = new ArrayList<String>();
        for (String sql : jdbc.executed) {
            if (sql.contains("ADD COLUMN")) {
                out.add(sql);
            }
        }
        return out;
    }

    /** 有行 + 必填 + 无默认值: 一列都不许多发。 */
    @Test
    public void requiredColumnWithoutDefaultIsNotAddedToAPopulatedTable() throws Exception {
        EntityDefDTO def = defWithRequired(false);
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "needs_value"));
        useEntityRows();                      // 这张表没有第二个主人 —— 红只能是这条规则给的
        jdbc.hasRows = Boolean.TRUE;

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals(ProvisionReport.FAILED, item.getStatus());
        assertTrue("要点名补不上的是哪一栏: " + item.getMissingColumns(),
                item.getMissingColumns().contains("needs_value"));
        assertTrue("要给出可操作的修法，而不是等库给一个引擎各自不同的答案: " + item.getMessage(),
                item.getMessage().contains("默认值") && item.getMessage().contains("已经有行"));
        // 这一句才是「不碰 DDL」的证据: 不是「发了但库拒了」，而是压根没发出去。
        assertTrue("必填无默认值的列不许被发进有行的表: " + executedAdds(jdbc),
                executedAdds(jdbc).isEmpty());
    }

    /** 负控: 分叉的依据是「有没有行」，不是「必不必填」—— 空表照旧补。 */
    @Test
    public void requiredColumnOnAnEmptyTableIsStillAdded() throws Exception {
        EntityDefDTO def = defWithRequired(false);
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "needs_value"));
        useEntityRows();
        jdbc.hasRows = Boolean.FALSE;
        jdbc.appliesAddColumn = true;

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals(ProvisionReport.ALTERED, item.getStatus());
        assertTrue("空表不存在「已有行被填成空串」这个问题, 该补就补: " + executedAdds(jdbc),
                executedAdds(jdbc).toString().contains("ADD COLUMN `needs_value`"));
    }

    /** 负控: 规则只管必填 —— 可空列补进有行的表是正常操作。 */
    @Test
    public void nullableColumnOnAPopulatedTableIsStillAdded() throws Exception {
        EntityDefDTO def = entityWith("seed", "note");
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "note"));
        useEntityRows();
        jdbc.hasRows = Boolean.TRUE;
        jdbc.appliesAddColumn = true;

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals("把可空列也一起拒掉，就是拿 #47 修好的能力换一个假安全: " + item.getMessage(),
                ProvisionReport.ALTERED, item.getStatus());
        assertTrue(executedAdds(jdbc).toString().contains("ADD COLUMN `note`"));
    }

    /** 配了默认值的必填列: 已有行拿到的必须是**声明过的那个**值，不是库的隐式空串。 */
    @Test
    public void requiredColumnWithDefaultCarriesItIntoTheAddColumnDdl() throws Exception {
        EntityDefDTO def = defWithRequired(true);
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "needs_value"));
        useEntityRows();
        jdbc.hasRows = Boolean.TRUE;
        jdbc.appliesAddColumn = true;

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals(ProvisionReport.ALTERED, item.getStatus());
        String add = executedAdds(jdbc).toString();
        assertTrue("该发出去的这一句要带上默认值: " + add, add.contains("ADD COLUMN `needs_value`"));
        assertTrue("已有行必须由 DEFAULT 接住，而不是靠库的隐式默认: " + add,
                add.contains("NOT NULL") && add.contains("DEFAULT '未填'"));
    }

    /** 问不到行数时的方向: 宁可多拦一句让用户配默认值，也不许在不知情下改已有数据。 */
    @Test
    public void unaskableRowCountFailsSafeToRefusal() throws Exception {
        EntityDefDTO def = defWithRequired(false);
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "needs_value"));
        useEntityRows();
        jdbc.hasRows = null;                  // 探针自己抛

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals("读不出有多少行时按「有行」处理: " + item.getMessage(),
                ProvisionReport.FAILED, item.getStatus());
        assertTrue(executedAdds(jdbc).isEmpty());
    }

    /**
     * 「配了默认值」的口径必须和 columnClause 一致: 空串落不进 DEFAULT 子句，就等于没配。
     * <p>
     * 这一条是 N5 的猎物: 把口径写成「非 null 就算配了」，这一栏会以「有默认值」的名义被发进库，
     * 而 DDL 里其实一个 DEFAULT 都没有 —— 于是回到 #54 那个静默填空串的旧坑。
     */
    @Test
    public void blankDefaultDoesNotCountAsADeclaredDefault() throws Exception {
        EntityDefDTO def = defWithDeclaredDefault("");
        FakeJdbc jdbc = useFakeJdbc(colsMissingOne(def, "needs_value"));
        useEntityRows();
        jdbc.hasRows = Boolean.TRUE;

        ProvisionReport.Item item = provisionOneOf(def);
        assertEquals("空串默认值进不了 DDL，这一栏等同没配: " + item.getMessage(),
                ProvisionReport.FAILED, item.getStatus());
        assertTrue(executedAdds(jdbc).isEmpty());
    }
}
