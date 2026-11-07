package com.zifang.z.lc.core.ai;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.core.event.EventService;
import com.zifang.z.lc.core.relation.RelationService;
import com.zifang.z.lc.core.relation.RelationServiceImpl;
import com.zifang.z.lc.core.schema.SchemaAdminBizService;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import com.zifang.z.lc.mapper.relation.RelationMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AiModelingService 单元测试
 * <p>
 * 两副夹具:
 * <ul>
 *   <li>{@code bareService}: 不注入任何依赖 (llmGatewayServiceRaw 保持 null)，
 *       走 generateModelSuggestion 的模板 fallback 路径；同时是「没有落库服务就不许报成功」的行为守卫。</li>
 *   <li>{@code service}: 注入<strong>真实的</strong> SchemaAdminBizService / RelationServiceImpl，
 *       只有 mapper 层用 JDK 动态代理 + 内存 store 顶住（与 SchemaAdminBizServiceTest 同款夹具）。
 *       于是 applyModelSuggestion 走的是生产落库代码，listEntities / listRelationsByApp 回读得到
 *       真正写进去的行，而不是替身自己记的一笔账。</li>
 * </ul>
 */
public class AiModelingServiceTest {

    private AiModelingService bareService;
    private AiModelingService service;
    private SchemaAdminService schemaAdmin;
    private RelationService relationService;

    private final Map<Long, Object> appStore = new LinkedHashMap<>();
    private final Map<Long, Object> entityStore = new LinkedHashMap<>();
    private final Map<Long, Object> fieldStore = new LinkedHashMap<>();
    private final Map<Long, Object> relationStore = new LinkedHashMap<>();

    @Before
    public void setUp() throws Exception {
        bareService = new AiModelingService();

        AtomicLong ids = new AtomicLong(0);
        LcAppEntityMapper appMapper = mapper(LcAppEntityMapper.class, appStore, ids);
        EntityMapper entityMapper = mapper(EntityMapper.class, entityStore, ids);
        FieldMapper fieldMapper = mapper(FieldMapper.class, fieldStore, ids);
        RelationMapper relationMapper = mapper(RelationMapper.class, relationStore, ids);

        SchemaAdminBizService schema = new SchemaAdminBizService(fakeDataSource());
        setField(schema, "lcAppEntityMapper", appMapper);
        setField(schema, "entityMapper", entityMapper);
        setField(schema, "fieldMapper", fieldMapper);
        setField(schema, "eventService", fakeEventService());
        schemaAdmin = schema;

        RelationServiceImpl rel = new RelationServiceImpl();
        setField(rel, "relationMapper", relationMapper);
        relationService = rel;

        service = new AiModelingService();
        setField(service, "schemaAdminService", schemaAdmin);
        setField(service, "relationService", relationService);

        // 预置应用: createEntity 会校验 app 存在
        seedApp("default", "crm");
    }

    // ===== 原有契约: LLM Gateway 缺失时的模板 fallback =====

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("AiModelingService 应当标注 @Service",
                AiModelingService.class.getAnnotation(Service.class));
    }

    @Test
    public void generateModelSuggestionShouldFallbackWithoutLlm() {
        Map<String, Object> suggestion = bareService.generateModelSuggestion("crm", "创建一个订单管理");

        assertNotNull(suggestion);
        assertEquals("crm", suggestion.get("appCode"));
        assertEquals(Boolean.FALSE, suggestion.get("llmAvailable"));
        assertEquals(Boolean.TRUE, suggestion.get("needsReview"));
        assertEquals(0.0, ((Number) suggestion.get("confidence")).doubleValue(), 0.0001);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void generateModelSuggestionShouldReturnEmptySuggestions() {
        Map<String, Object> suggestion = bareService.generateModelSuggestion("crm", "库存系统");

        List<String> entities = (List<String>) suggestion.get("suggestedEntities");
        List<String> relations = (List<String>) suggestion.get("suggestedRelations");
        assertNotNull(entities);
        assertNotNull(relations);
        assertTrue(entities.isEmpty());
        assertTrue(relations.isEmpty());
    }

    @Test
    public void generateModelSuggestionShouldKeepDescription() {
        Map<String, Object> suggestion = bareService.generateModelSuggestion("crm", "员工考勤");

        assertEquals("员工考勤", suggestion.get("description"));
    }

    @Test
    public void generateModelSuggestionShouldHandleNullDescription() {
        Map<String, Object> suggestion = bareService.generateModelSuggestion("crm", null);

        assertNotNull(suggestion);
        assertFalse((Boolean) suggestion.get("llmAvailable"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.ai",
                AiModelingService.class.getPackage().getName());
    }

    // ===== 缺陷 013 §3 / §4.4: 停止「只解析名字就 success=true」的假成功 =====

    /** A 方案主契约: 建议里的实体 + 字段真的落库，回读得到；createdXxx 只报真正成功的项。 */
    @Test
    public void applySuggestionShouldPersistEntitiesAndFields() {
        Map<String, Object> s = suggestion("entities", Arrays.asList(
                entity("customer", Arrays.asList(field("name", "STRING"), field("phone", "STRING")))));

        Map<String, Object> r = service.applyModelSuggestion("crm", s);

        assertEquals(Boolean.TRUE, r.get("success"));
        assertEquals("default", r.get("tenantCode"));          // 未传 tenantCode -> 仓内默认口径
        assertEquals(Arrays.asList("customer"), r.get("createdEntities"));
        List<String> createdFields = strings(r, "createdFields");
        assertTrue("字段必须来自落库回读结果: " + createdFields, createdFields.contains("customer.name"));
        assertTrue("字段必须来自落库回读结果: " + createdFields, createdFields.contains("customer.phone"));

        // 回读: 走生产 SchemaAdminBizService.listEntities，从内存 mapper store 里把行取出来
        List<EntityDefDTO> rows = schemaAdmin.listEntities("default", "crm");
        assertEquals("实体应当真的写进 z_lc_entity", 1, rows.size());
        assertEquals("customer", rows.get(0).getEntityCode());
        assertEquals("default", rows.get(0).getTenantCode());
        List<FieldDefDTO> stored = rows.get(0).getFields();
        assertNotNull("字段应当真的写进 z_lc_field", stored);
        Set<String> codes = new HashSet<>();
        for (FieldDefDTO f : stored) {
            codes.add(f.getFieldCode());
        }
        assertTrue("name 落库了: " + codes, codes.contains("name"));
        assertTrue("phone 落库了: " + codes, codes.contains("phone"));
    }

    /** 关系也真的落库（z_lc_relation），不是只在 createdRelations 里记个名字。 */
    @Test
    public void applySuggestionShouldPersistRelations() {
        Map<String, Object> s = suggestion("suggestedRelations", Arrays.asList(relation("cust_order")));

        Map<String, Object> r = service.applyModelSuggestion("crm", s);

        assertEquals(Boolean.TRUE, r.get("success"));
        assertEquals(Arrays.asList("cust_order"), r.get("createdRelations"));
        List<RelationDTO> rows = relationService.listRelationsByApp("default", "crm");
        assertEquals("关系应当真的写进 z_lc_relation", 1, rows.size());
        assertEquals("cust_order", rows.get(0).getRelationCode());
        assertEquals("ONE_TO_MANY", rows.get(0).getRelationType());
    }

    /** 重复建议幂等: 第二次不再插入，且 createdXxx 为空、skippedXxx 点名跳过项。 */
    @Test
    public void applySuggestionShouldBeIdempotentOnRepeat() {
        Map<String, Object> s = suggestion(
                "entities", Arrays.asList(entity("customer", Arrays.asList(field("name", "STRING")))),
                "suggestedRelations", Arrays.asList(relation("cust_order")));

        Map<String, Object> first = service.applyModelSuggestion("crm", s);
        assertEquals(Boolean.TRUE, first.get("success"));

        Map<String, Object> second = service.applyModelSuggestion("crm", s);
        assertEquals(Boolean.TRUE, second.get("success"));
        assertTrue("重复调用不得再报创建成功", strings(second, "createdEntities").isEmpty());
        assertTrue("重复调用不得再报创建成功", strings(second, "createdFields").isEmpty());
        assertTrue("重复调用不得再报创建成功", strings(second, "createdRelations").isEmpty());
        assertTrue("重复实体要计入 skippedEntities", strings(second, "skippedEntities").contains("customer"));
        assertTrue("重复关系要计入 skippedRelations", strings(second, "skippedRelations").contains("cust_order"));
        assertEquals("库里仍然只有一份实体", 1, schemaAdmin.listEntities("default", "crm").size());
        assertEquals("库里仍然只有一份关系", 1, relationService.listRelationsByApp("default", "crm").size());
    }

    /** suggestion 里显式 tenantCode 必须原样透传到落库服务，不能被写死成 default。 */
    @Test
    public void applySuggestionShouldHonourExplicitTenantCode() {
        seedApp("t9", "crm");
        Map<String, Object> s = suggestion(
                "tenantCode", "t9",
                "entities", Arrays.asList(entity("order", Arrays.asList(field("amount", "DECIMAL")))));

        Map<String, Object> r = service.applyModelSuggestion("crm", s);

        assertEquals(Boolean.TRUE, r.get("success"));
        assertEquals("t9", r.get("tenantCode"));
        assertEquals("t9 租户下要查得到", 1, schemaAdmin.listEntities("t9", "crm").size());
        assertTrue("不能误写到 default 租户", schemaAdmin.listEntities("default", "crm").isEmpty());
    }

    /** 非法建议一律 success=false + reason + failedItem，且不返回任何 createdXxx 假数据。 */
    @Test
    public void applySuggestionShouldRejectIllegalInputs() {
        Map<String, Object> valid = suggestion("entities",
                Arrays.asList(entity("customer", Arrays.asList(field("name", "STRING")))));

        assertFailed(service.applyModelSuggestion(null, valid));                 // 缺 appCode
        assertFailed(service.applyModelSuggestion("   ", valid));               // 空白 appCode
        assertFailed(service.applyModelSuggestion("crm", null));                // 缺 suggestion
        assertFailed(service.applyModelSuggestion("crm", suggestion("entities", "not-an-array")));
        assertFailed(service.applyModelSuggestion("crm", suggestion("entities",
                Arrays.asList(new HashMap<String, Object>()))));                 // 实体缺 entityCode
        assertFailed(service.applyModelSuggestion("crm", suggestion("suggestedRelations",
                Arrays.asList(new HashMap<String, Object>()))));                 // 关系缺 relationCode
        assertTrue("非法建议不得落库", schemaAdmin.listEntities("default", "crm").isEmpty());
    }

    /** 非法字段编码由落库服务在写入处拒绝；异常必须如实转成失败，不得报成功或留半条实体。 */
    @Test
    public void applySuggestionShouldRejectIllegalFieldCode() {
        Map<String, Object> s = suggestion("entities", Arrays.asList(
                entity("customer", Arrays.asList(field("2bad", "STRING")))));

        Map<String, Object> r = service.applyModelSuggestion("crm", s);

        assertFailed(r);
        assertTrue("reason 要点名被拒的编码: " + r.get("reason"),
                String.valueOf(r.get("reason")).contains("2bad"));
        assertTrue("校验在写入之前，库里不该有实体", schemaAdmin.listEntities("default", "crm").isEmpty());
    }

    /** 落库调用抛异常 (app 不存在) 时绝不能吞成 success=true 的「部分成功」。 */
    @Test
    public void applySuggestionShouldFailLoudlyWhenPersistenceThrows() {
        Map<String, Object> s = suggestion("entities", Arrays.asList(
                entity("customer", Arrays.asList(field("name", "STRING")))));

        Map<String, Object> r = service.applyModelSuggestion("ghost", s);   // 未预置 ghost 应用

        assertFailed(r);
        assertTrue("reason 要带出落库服务的真实原因: " + r.get("reason"),
                String.valueOf(r.get("reason")).contains("App not found"));
        assertTrue(entityStore.isEmpty());
    }

    /**
     * 行为守卫: 没有落库依赖时绝不许报成功。
     * <p>
     * 谁要是把 applyModelSuggestion 改回「只把名字收进 List 就 success=true」，这条必红 ——
     * 原来的实现在这里会返回 success=true 且 createdEntities=["customer"]。
     */
    @Test
    public void applySuggestionShouldNotReportSuccessWithoutPersistenceLayer() {
        Map<String, Object> s = suggestion("entities", Arrays.asList(
                entity("customer", Arrays.asList(field("name", "STRING")))));

        Map<String, Object> r = bareService.applyModelSuggestion("crm", s);

        assertFailed(r);
        assertTrue("reason 要说明缺落库服务: " + r.get("reason"),
                String.valueOf(r.get("reason")).contains("落库服务未注入"));
        assertTrue("没有落库服务时库里当然什么都没有",
                schemaAdmin.listEntities("default", "crm").isEmpty());
    }

    /**
     * 源码守卫: 生产源码里不许再出现裸的假成功写法，且必须真的调用落库服务。
     * <p>
     * 用 class 的 codeSource 反推模块根目录，与 surefire 的 working directory 无关。
     */
    @Test
    public void sourceGuardShouldForbidBareFakeSuccess() throws Exception {
        URL loc = AiModelingService.class.getProtectionDomain().getCodeSource().getLocation();
        File classesDir = new File(loc.toURI());
        File src = new File(classesDir.getParentFile().getParentFile(),
                "src/main/java/com/zifang/z/lc/core/ai/AiModelingService.java");
        assertTrue("源码守卫: 找不到 " + src, src.isFile());
        String source = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);

        assertFalse("源码守卫: 不得再出现裸的 result.put(\"success\", true) 假成功",
                source.contains("result.put(\"success\", true)"));
        assertTrue("源码守卫: applyModelSuggestion 必须真的调用落库服务 createEntity",
                source.contains("schemaAdminService.createEntity("));
        assertFalse("源码守卫: 账本点名的死 mapper 不该再被 import 回来",
                source.contains("import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;"));
    }

    // ===== 夹具 =====

    private void seedApp(String tenantCode, String appCode) {
        AppDTO req = new AppDTO();
        req.setTenantCode(tenantCode);
        req.setAppCode(appCode);
        req.setAppName(appCode.toUpperCase());
        schemaAdmin.createApp(req);
    }

    private static void assertFailed(Map<String, Object> r) {
        assertEquals(Boolean.FALSE, r.get("success"));
        assertNotNull("失败必须有 reason", r.get("reason"));
        assertFalse("reason 不能是空串", String.valueOf(r.get("reason")).trim().isEmpty());
        assertNotNull("失败必须点名 failedItem", r.get("failedItem"));
        assertTrue("失败时不得返回 createdEntities 假数据", strings(r, "createdEntities").isEmpty());
        assertTrue("失败时不得返回 createdFields 假数据", strings(r, "createdFields").isEmpty());
        assertTrue("失败时不得返回 createdRelations 假数据", strings(r, "createdRelations").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> result, String key) {
        return (List<String>) result.get(key);
    }

    private static Map<String, Object> suggestion(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> entity(String code, List<Map<String, Object>> fields) {
        Map<String, Object> e = new HashMap<>();
        e.put("entityCode", code);
        e.put("entityName", code + "名称");
        e.put("fields", fields);
        return e;
    }

    private static Map<String, Object> field(String code, String type) {
        Map<String, Object> f = new HashMap<>();
        f.put("fieldCode", code);
        f.put("fieldName", code);
        f.put("fieldType", type);
        f.put("required", Boolean.FALSE);
        f.put("fieldLength", 64);
        return f;
    }

    private static Map<String, Object> relation(String code) {
        Map<String, Object> r = new HashMap<>();
        r.put("relationCode", code);
        r.put("relationName", code);
        r.put("sourceEntityCode", "customer");
        r.put("targetEntityCode", "order");
        r.put("relationType", "ONE_TO_MANY");
        r.put("sourceFieldCode", "id");
        return r;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(Class<T> iface, Map<Long, Object> store, AtomicLong ids) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(),
                new Class<?>[]{iface, BaseMapper.class}, new StoreHandler(store, ids));
    }

    private static DataSource fakeDataSource() {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
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
    }

    private static EventService fakeEventService() {
        return (EventService) Proxy.newProxyInstance(
                EventService.class.getClassLoader(), new Class<?>[]{EventService.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        // getLastEvent/append 返回 null: createEntity 的 emit 在 try-catch 里，安全
                        return null;
                    }
                });
    }

    /**
     * 内存 BaseMapper 替身: insert/updateById/selectById 真存真取，
     * select* 按 QueryWrapper 里的 eq 谓词过滤（照抄 SchemaAdminBizServiceTest 的判法）。
     * 不按谓词过滤的话，"重复检测"和"按租户回读"在单测层就全是假的。
     */
    private static final class StoreHandler implements InvocationHandler {
        private static final Pattern EQ = Pattern.compile(
                "(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}");

        private final Map<Long, Object> store;
        private final AtomicLong ids;

        StoreHandler(Map<Long, Object> store, AtomicLong ids) {
            this.store = store;
            this.ids = ids;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("insert".equals(name)) {
                Object row = args[0];
                setId(row, ids.incrementAndGet());
                store.put(getId(row), row);
                return 1;
            }
            if ("updateById".equals(name)) {
                Object row = args[0];
                store.put(getId(row), row);
                return 1;
            }
            if ("deleteById".equals(name)) {
                store.remove(((Number) args[0]).longValue());
                return 1;
            }
            if ("selectById".equals(name)) {
                return store.get(((Number) args[0]).longValue());
            }
            if ("selectOne".equals(name)) {
                List<Object> rows = match(args.length > 0 ? args[0] : null);
                return rows.isEmpty() ? null : rows.get(0);
            }
            if ("selectList".equals(name)) {
                return match(args.length > 0 ? args[0] : null);
            }
            if ("selectCount".equals(name)) {
                return (long) match(args.length > 0 ? args[0] : null).size();
            }
            return defaultValue(method.getReturnType());
        }

        private List<Object> match(Object wrapper) {
            Map<String, Object> eq = predicates(wrapper);
            List<Object> rows = new ArrayList<>();
            for (Object row : store.values()) {
                boolean ok = true;
                for (Map.Entry<String, Object> e : eq.entrySet()) {
                    Object actual = get(row, e.getKey());
                    if (actual == null || !String.valueOf(actual).equals(String.valueOf(e.getValue()))) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    rows.add(row);
                }
            }
            return rows;
        }

        private static Map<String, Object> predicates(Object wrapper) {
            Map<String, Object> out = new LinkedHashMap<>();
            if (!(wrapper instanceof AbstractWrapper)) {
                return out;
            }
            AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
            Matcher ma = EQ.matcher(String.valueOf(aw.getSqlSegment()));
            while (ma.find()) {
                Object v = aw.getParamNameValuePairs().get(ma.group(2));
                if (v != null) {
                    out.put(ma.group(1), v);
                }
            }
            return out;
        }

        private static Object get(Object row, String column) {
            String camel = toCamel(column);
            try {
                Method g = row.getClass().getMethod(
                        "get" + Character.toUpperCase(camel.charAt(0)) + camel.substring(1));
                return g.invoke(row);
            } catch (Exception ex) {
                return null;
            }
        }

        private static String toCamel(String column) {
            StringBuilder sb = new StringBuilder();
            boolean upperNext = false;
            for (char c : column.toCharArray()) {
                if (c == '_') {
                    upperNext = true;
                } else if (upperNext) {
                    sb.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static void setId(Object row, long id) {
            try {
                row.getClass().getMethod("setId", Long.class).invoke(row, Long.valueOf(id));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static long getId(Object row) {
            try {
                return ((Number) row.getClass().getMethod("getId").invoke(row)).longValue();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static Object defaultValue(Class<?> rt) {
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
            return null;
        }
    }
}
