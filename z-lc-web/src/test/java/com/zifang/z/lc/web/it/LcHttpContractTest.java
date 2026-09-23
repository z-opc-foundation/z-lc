package com.zifang.z.lc.web.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.core.mapper.DbTableMapperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * z-lc HTTP 契约集成测试 —— 真起 Spring 上下文、真打 MockMvc、真查 H2.
 * <p>
 * 这个文件存在的意义：仓里原有 1200+ 个测试几乎全是 DTO getter/setter 往返，
 * 对 HTTP 层与 SQL 层零覆盖，于是下面这几个真实缺陷全都「测试全绿」地存在了很久：
 * <ul>
 *   <li>字典项新增/更新走了整表替换语义 —— 新增一项会删光同字典其它项</li>
 *   <li>list/count SQL 的 JOIN 占位符与参数入队顺序不一致 —— tenant 与 dictCode 互相绑错，
 *       含字典字段的实体分页恒为 0 条，租户过滤实际失效</li>
 *   <li>COUNT 语句带 ORDER BY —— 只在 MySQL 的宽松模式下不报错</li>
 *   <li>结构化排序把 id / create_time 等系统列判成未知字段</li>
 * </ul>
 * 所以这里的断言都是对着缺陷写的，改回去就会红。
 */
@SpringBootTest(classes = LcTestApplication.class)
@AutoConfigureMockMvc
class LcHttpContractTest {

    private static final String APP = "demo";
    private static final String ENTITY = "order";
    private static final String TENANT = "default";
    private static final String DICT = "order_status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private javax.sql.DataSource dataSource;

    /** 上下文里所有连接池（spring.datasource 与 dataSourceLc 是两个不同实现）。 */
    @Autowired
    private List<javax.sql.DataSource> allDataSources;

    /** 测试上下文里没有 JdbcTemplate bean, 但有 DataSource —— 自己包一层. */
    private org.springframework.jdbc.core.JdbcTemplate jdbc() {
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }

    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("缺陷#5 回归：重复字典项不得把业务记录 fan-out 成多行")
    void duplicateDictItemsDoNotFanOutRows() throws Exception {
        JsonNode clean = post("/api/lc/runtime/list", "{\"page\":1,\"size\":50}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(clean, "runtime list before dup");
        long cleanTotal = clean.path("data").path("total").asLong();
        assertTrue(cleanTotal >= 2, "seed needs >= 2 rows, got " + cleanTotal);
        long cleanRows = clean.path("data").path("records").size();
        assertTrue(cleanRows >= 2, "seed needs >= 2 records, got " + cleanRows);

        // z_lc_dict_item 没有唯一索引 (软删语义和 uk 冲突), 所以重复项既可能来自旧库脏数据,
        // 也可能来自绕过 API 的直接写入。读路径必须自己保证 1:1, 否则 total/分页/页脚统计全虚高。
        int injected = jdbc().update(
                "INSERT INTO z_lc_dict_item (tenant_code, dict_code, item_code, item_label, item_value,"
                        + " sort_order, deleted, create_time, update_time)"
                        + " SELECT tenant_code, dict_code, item_code, item_label, item_value, sort_order,"
                        + " 0, create_time, update_time FROM z_lc_dict_item WHERE dict_code = ? AND deleted = 0",
                DICT);
        assertTrue(injected >= 1, "dup insert should have run, got " + injected);
        try {
            JsonNode dup = post("/api/lc/runtime/list", "{\"page\":1,\"size\":50}",
                    "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
            assertOk(dup, "runtime list after dup");
            assertEquals(cleanTotal, dup.path("data").path("total").asLong(),
                    "每个 (dict_code,item_code) 只应折叠出一行, total 不得随字典重复项翻倍");
            assertEquals(cleanRows, dup.path("data").path("records").size(),
                    "records 数量不得被 join fan-out: " + dup.path("data").path("records"));

            boolean sawLabel = false;
            for (JsonNode row : dup.path("data").path("records")) {
                String label = row.path("status_label").asText("");
                if (!label.isEmpty()) {
                    assertTrue("Done".equals(label) || "Pending".equals(label),
                            "折叠后仍要解析得出 label, got " + label);
                    sawLabel = true;
                }
            }
            assertTrue(sawLabel, "label must survive the dedupe subquery: " + dup.path("data"));
        } finally {
            jdbc().update("DELETE FROM z_lc_dict_item WHERE dict_code = ? AND id > (SELECT MIN(id) FROM z_lc_dict_item WHERE dict_code = ?)",
                    DICT, DICT);
        }
    }

    @Test
    @DisplayName("缺陷#5 回归：API 不接受同字典下的重复 item_code")
    void addItemRejectsDuplicateItemCode() throws Exception {
        String dictCode = "dup_guard_" + System.currentTimeMillis() % 1000000;
        assertOk(post("/api/lc/dict/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"dictCode\":\"" + dictCode
                        + "\",\"dictName\":\"重复守卫\",\"status\":\"ENABLED\"}"), "dict create");

        String body = "{\"tenantCode\":\"" + TENANT + "\",\"dictCode\":\"" + dictCode
                + "\",\"itemCode\":\"A\",\"itemLabel\":\"甲\",\"itemValue\":\"A\",\"sortOrder\":1}";
        assertOk(post("/api/lc/dict/items/create", body, "dictCode", dictCode), "first add");

        JsonNode second = post("/api/lc/dict/items/create", body, "dictCode", dictCode);
        assertFalse(second.path("success").asBoolean(),
                "同 item_code 二次新增必须被拒, 否则 join fan-out: " + second);
        assertEquals(400, second.path("code").asInt(), "duplicate item code should map to 400: " + second);

        JsonNode items = get("/api/lc/dict/items", "dictCode", dictCode, "tenantCode", TENANT);
        assertOk(items, "items list");
        assertEquals(1, items.path("data").size(), "rejected add must not have persisted: " + items);

        // 改名撞号同样要拦
        JsonNode other = post("/api/lc/dict/items/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"dictCode\":\"" + dictCode
                        + "\",\"itemCode\":\"B\",\"itemLabel\":\"乙\",\"sortOrder\":2}",
                "dictCode", dictCode);
        assertOk(other, "add B");
        Long bId = other.path("data").path("id").asLong();
        JsonNode renamed = post("/api/lc/dict/items/update",
                "{\"id\":" + bId + ",\"tenantCode\":\"" + TENANT + "\",\"dictCode\":\"" + dictCode
                        + "\",\"itemCode\":\"A\",\"itemLabel\":\"乙\",\"sortOrder\":2}",
                "dictCode", dictCode);
        assertFalse(renamed.path("success").asBoolean(),
                "把 B 改名成已存在的 A 也必须被拒: " + renamed);
    }

    @Test
    @DisplayName("重复创建字典是业务错误，不是 500；软删后占着唯一索引的同样要说清楚")
    void duplicateDictCreateIsABusinessError() throws Exception {
        String dictCode = "dup_dict_" + System.currentTimeMillis() % 1000000;
        String body = "{\"tenantCode\":\"" + TENANT + "\",\"dictCode\":\"" + dictCode
                + "\",\"dictName\":\"重复字典\",\"status\":\"ENABLED\"}";
        JsonNode first = post("/api/lc/dict/create", body);
        assertOk(first, "first dict create");
        long dictId = first.path("data").path("id").asLong();
        assertTrue(dictId > 0, "create 应返回 id: " + first);

        JsonNode again = post("/api/lc/dict/create", body);
        assertFalse(again.path("success").asBoolean(), "同 dictCode 二次创建必须失败: " + again);
        assertEquals(400, again.path("code").asInt(),
                "唯一键冲突要走业务校验，不能落到 500 兜底: " + again);
        String message = again.path("message").asText("");
        assertTrue(message.contains("字典已存在"), "错误要说清是编码重复: " + again);
        assertFalse(message.toUpperCase().contains("SELECT"), "不得把 SQL 片段透出给前端: " + message);

        // 软删的字典仍占着 uk_dict_tenant_code(索引不含 deleted)。
        // 预检若只看 deleted=0，这里就会又变成一次 500。
        assertOk(post("/api/lc/dict/delete", "{\"id\":" + dictId + "}"), "soft delete dict");
        JsonNode afterDelete = post("/api/lc/dict/create", body);
        assertFalse(afterDelete.path("success").asBoolean(),
                "软删之后同 code 依然撞唯一索引，必须还是业务错误: " + afterDelete);
        assertEquals(400, afterDelete.path("code").asInt(), "应为 400: " + afterDelete);
        assertTrue(afterDelete.path("message").asText("").contains("已被删除"),
                "要告诉调用方是旧字典占着编码，而不是一个含糊的重复: " + afterDelete);
    }

    @Test
    @DisplayName("唯一编码撞车：关系与应用都给 400，且绝不把索引名/列名透给前端")
    void duplicateUniqueCodesAreBusinessErrorsWithoutLeakingSchema() throws Exception {
        long stamp = System.currentTimeMillis() % 1000000;

        String relCode = "duprel" + stamp;
        String rel = "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + APP + "\",\"relationCode\":\"" + relCode
                + "\",\"relationName\":\"重复关系\",\"sourceEntityCode\":\"" + ENTITY
                + "\",\"targetEntityCode\":\"" + ENTITY + "\",\"relationType\":\"ONE_TO_MANY\"}";
        assertOk(post("/api/lc/relation/create", rel), "first relation");
        JsonNode relDup = post("/api/lc/relation/create", rel);
        assertFalse(relDup.path("success").asBoolean(), "同 relationCode 二次创建必须失败: " + relDup);
        assertEquals(400, relDup.path("code").asInt(),
                "撞 uk_relation_tenant_code 要走业务校验; 修复前这里是 500: " + relDup);
        String relMsg = relDup.path("message").asText("");
        assertTrue(relMsg.contains("关系已存在"), "错误要指名是关系重复: " + relDup);
        assertNoSchemaLeak(relMsg);

        // 应用：删掉以后再建同一个 code。软删行仍占唯一索引，而预检以前只看 deleted=0。
        String appCode = "dupapp" + stamp;
        String appBody = "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + appCode + "\",\"appName\":\"重复应用\"}";
        assertOk(post("/api/lc/app/create", appBody), "create app");
        assertOk(post("/api/lc/app/delete", "{\"appCode\":\"" + appCode + "\"}"), "soft delete app");
        JsonNode reborn = post("/api/lc/app/create", appBody);
        assertFalse(reborn.path("success").asBoolean(),
                "软删后同 appCode 依然撞唯一索引，不能再报成功: " + reborn);
        assertEquals(400, reborn.path("code").asInt(),
                "修复前这里是 500 + H2 原文: " + reborn);
        String appMsg = reborn.path("message").asText("");
        assertTrue(appMsg.contains("已被删除"),
                "要解释为什么\"已存在\"的是刚被删掉的应用: " + reborn);
        assertNoSchemaLeak(appMsg);
    }

    /** 唯一键冲突的兜底文案里不许出现物理表名、索引名或 SQL 片段。 */
    private static void assertNoSchemaLeak(String message) {
        String upper = message.toUpperCase();
        assertFalse(upper.contains("UNIQUE INDEX") || upper.contains("UK_")
                        || upper.contains("Z_LC_") || upper.contains("PRIMARY KEY"),
                "把数据库细节透给了前端: " + message);
    }

    /* ------------------------------------------------------------------ */

    /**
     * MockMvc 的 content(String) 与 getContentAsString() 不带字符集时按 ISO-8859-1 处理，
     * 于是中文请求体进去、中文错误信息出来都会变乱码 —— 之前有几个断言其实是拿着被搞坏的字节在跑。
     * 这里统一强制 UTF-8，顺带把"接口以 UTF-8 收发中文"这件事也纳入验证。
     */
    private JsonNode call(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(body == null || body.isEmpty(), "empty body for " + builder);
        return json.readTree(body);
    }

    private JsonNode get(String path, String... kv) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(path);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            builder = builder.param(kv[i], kv[i + 1]);
        }
        return call(builder);
    }

    private JsonNode post(String path, Object body, String... kv) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(path)
                .content(body instanceof String ? (String) body : json.writeValueAsString(body));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            builder = builder.param(kv[i], kv[i + 1]);
        }
        return call(builder);
    }

    private static void assertOk(JsonNode envelope, String context) {
        assertTrue(envelope.path("success").asBoolean(),
                context + " should succeed, got code=" + envelope.path("code").asText()
                        + " message=" + envelope.path("message").asText());
    }

    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Result 信封形状稳定，前端 client.ts 依赖它")
    void envelopeShapeIsStable() throws Exception {
        JsonNode node = get("/api/lc/health");
        assertOk(node, "health");
        for (String field : new String[]{"data", "success", "code", "message"}) {
            assertTrue(node.has(field), "envelope must carry `" + field + "`, got " + node);
        }
        assertEquals("UP", node.path("data").path("status").asText());
    }

    @Test
    @DisplayName("缺陷#2 回归：dict JOIN 的占位符不得和 tenant 参数互相绑错")
    void runtimeListWithDictFieldReturnsRowsAndLabels() throws Exception {
        JsonNode node = post("/api/lc/runtime/list", "{\"page\":1,\"size\":20}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(node, "runtime list");
        JsonNode page = node.path("data");
        // 绑错时这里恒为 0 —— 就是当初线上"含字典字段的实体分页永远没有数据"的现象
        assertTrue(page.path("total").asLong() >= 2,
                "seeded rows must be visible through the dict JOIN, got " + page);

        boolean sawLabel = false;
        for (JsonNode row : page.path("records")) {
            assertTrue(row.has("id"), "system column id must come back: " + row);
            String label = row.path("status_label").asText("");
            if (!label.isEmpty()) {
                assertTrue("Done".equals(label) || "Pending".equals(label),
                        "status_label must resolve through the dict, got " + label);
                sawLabel = true;
            }
        }
        assertTrue(sawLabel, "at least one row should carry the derived status_label: " + page);
    }

    @Test
    @DisplayName("legacy filters 后缀算子与结构化 conditions 共用同一套语义")
    void filterOperatorsWork() throws Exception {
        JsonNode eq = post("/api/lc/runtime/list", "{\"page\":1,\"size\":20,\"filters\":{\"status\":\"OK\"}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(eq, "filter eq");
        assertTrue(eq.path("data").path("total").asLong() >= 1, "eq filter should hit Alice: " + eq.path("data"));

        JsonNode gte = post("/api/lc/runtime/list", "{\"page\":1,\"size\":20,\"filters\":{\"amount:gte\":60}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(gte, "filter gte");
        assertTrue(gte.path("data").path("total").asLong() >= 1, "gte filter should hit Alice: " + gte.path("data"));

        JsonNode structured = post("/api/lc/runtime/list",
                "{\"page\":1,\"size\":20,\"conjunction\":\"AND\",\"conditions\":[{\"fieldCode\":\"amount\",\"operator\":\"lt\",\"value\":60}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(structured, "structured conditions");
        assertTrue(structured.path("data").path("total").asLong() >= 1,
                "structured lt should hit Bob: " + structured.path("data"));
    }

    @Test
    @DisplayName("缺陷#4 回归：分页 count 不得带 ORDER BY，且 PageResult 形状不变")
    void pagedAppListSurvivesStrictSql() throws Exception {
        JsonNode node = post("/api/lc/admin/app/list", "{}", "page", "1", "size", "20");
        assertOk(node, "admin app list");
        JsonNode page = node.path("data");
        for (String field : new String[]{"records", "total", "pageNum", "pageSize"}) {
            assertTrue(page.has(field), "PageResult must carry `" + field + "`, got " + page);
        }
        assertTrue(page.path("total").asLong() >= 1, "seeded app should be listed: " + page);
    }

    @Test
    @DisplayName("缺陷#1 回归：新增字典项不得删掉同字典的其它字典项")
    void createDictItemIsNotDestructive() throws Exception {
        List<String> before = dictItemCodes();
        assertTrue(before.size() >= 2, "seed should give OK + PENDING, got " + before);

        JsonNode created = post("/api/lc/dict/items/create",
                "{\"dictCode\":\"" + DICT + "\",\"itemCode\":\"CANCELLED\",\"itemLabel\":\"已取消\",\"itemValue\":\"CANCELLED\",\"sortOrder\":99}",
                "dictCode", DICT);
        assertOk(created, "create dict item");

        List<String> after = dictItemCodes();
        assertTrue(after.containsAll(before),
                "creating one item must keep the existing ones (this used to wipe them): " + before + " -> " + after);
        assertTrue(after.contains("CANCELLED"), "new item should be present: " + after);
        assertEquals(before.size() + 1, after.size(), "exactly one item should have been added: " + after);
    }

    private List<String> dictItemCodes() throws Exception {
        JsonNode node = get("/api/lc/dict/items", "dictCode", DICT);
        assertOk(node, "dict items");
        List<String> codes = new ArrayList<String>();
        for (JsonNode item : node.path("data")) {
            codes.add(item.path("itemCode").asText());
        }
        return codes;
    }

    @Test
    @DisplayName("字段类型注册表把 10 种 fieldType 的能力一次性吐给前端")
    void fieldTypeDescriptorsAreComplete() throws Exception {
        JsonNode node = get("/api/lc/meta/field-types");
        assertOk(node, "field-types");
        JsonNode types = node.path("data");
        assertTrue(types.isArray() && types.size() >= 10, "expected >=10 descriptors, got " + types);

        List<String> names = new ArrayList<String>();
        JsonNode decimal = null;
        for (JsonNode item : types) {
            names.add(item.path("fieldType").asText());
            if ("DECIMAL".equals(item.path("fieldType").asText())) {
                decimal = item;
            }
        }
        for (String expected : new String[]{"STRING", "INT", "LONG", "DECIMAL", "BOOLEAN",
                "DATE", "DATETIME", "TEXT", "JSON", "REF"}) {
            assertTrue(names.contains(expected), "missing descriptor for " + expected + ": " + names);
        }
        assertNotNull(decimal);
        for (String field : new String[]{"cellValueType", "dbType", "widget", "operators",
                "sortable", "groupable", "filterable", "inlineEditable"}) {
            assertTrue(decimal.has(field), "descriptor must carry `" + field + "`, got " + decimal);
        }
        assertEquals("Number", decimal.path("cellValueType").asText());
    }

    @Test
    @DisplayName("meta bundle 一次给齐工作区要的全部元数据")
    void metaBundleCarriesWholeWorkspace() throws Exception {
        JsonNode node = get("/api/lc/meta/bundle", "appCode", APP, "tenantCode", TENANT);
        assertOk(node, "meta bundle");
        JsonNode bundle = node.path("data");
        assertEquals(APP, bundle.path("app").path("appCode").asText(), "bundle should carry the app");
        assertTrue(bundle.path("entities").size() >= 1, "bundle should carry entities: " + bundle.path("entities"));
        assertTrue(bundle.path("dicts").size() >= 1, "bundle should carry dicts");
        assertTrue(bundle.path("views").size() >= 1, "bundle should carry views");
        assertTrue(bundle.path("relations").size() >= 1, "bundle should carry relations");
        assertTrue(bundle.path("fieldTypes").size() >= 10, "bundle should carry fieldTypes");
    }

    @Test
    @DisplayName("不带 appCode 的 bundle 只回 fieldTypes，不得 500")
    void metaBundleDegradesWithoutAppCode() throws Exception {
        JsonNode node = get("/api/lc/meta/bundle");
        assertOk(node, "bundle without appCode");
        assertTrue(node.path("data").path("fieldTypes").size() >= 10, "should still serve descriptors");
    }

    @Test
    @DisplayName("事件回放折叠出实体定义，字典绑定关系不丢")
    void schemaReplayKeepsFieldMetadata() throws Exception {
        JsonNode node = get("/api/lc/app/schema", "appCode", APP, "tenantCode", TENANT);
        assertOk(node, "app schema");
        JsonNode order = null;
        for (JsonNode entity : node.path("data")) {
            if (ENTITY.equals(entity.path("entityCode").asText())) {
                order = entity;
            }
        }
        assertNotNull(order, "replayed schema should contain the seeded entity: " + node.path("data"));
        assertEquals("lc_demo_order", order.path("tableName").asText());
        JsonNode status = null;
        for (JsonNode field : order.path("fields")) {
            if ("status".equals(field.path("fieldCode").asText())) {
                status = field;
            }
        }
        assertNotNull(status, "status field must survive replay");
        assertEquals(DICT, status.path("dictCode").asText(),
                "dictCode must survive replay — the grid's label JOIN depends on it");
    }

    @Test
    @DisplayName("缺陷#5 回归：系统列可排序，未知列必须被白名单挡掉")
    void structuredSortWhitelist() throws Exception {
        JsonNode ok = post("/api/lc/runtime/list",
                "{\"page\":1,\"size\":10,\"sorts\":[{\"fieldCode\":\"id\",\"dir\":\"desc\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(ok, "sort by system column id");

        JsonNode bad = post("/api/lc/runtime/list",
                "{\"page\":1,\"size\":10,\"sorts\":[{\"fieldCode\":\"1=1\",\"dir\":\"asc\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(bad.path("success").asBoolean(), "unknown sort field must be rejected, got " + bad);
    }

    @Test
    @DisplayName("ORDER BY / filter key 的注入串不得进 SQL")
    void injectionNeverReachesSql() throws Exception {
        JsonNode stacked = post("/api/lc/runtime/list",
                "{\"page\":1,\"size\":10,\"orderBy\":\"id; DROP TABLE z_lc_app\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(stacked.toString().contains("DROP TABLE"), "injection text must not echo back as data");

        // 表还在 —— 注入没有真的执行
        JsonNode health = get("/api/lc/app/list");
        assertOk(health, "app list still works after injection attempts");
    }

    @Test
    @DisplayName("未知实体走信封报错，不是 HTTP 5xx 也不是 HTML 栈")
    void unknownEntityReportsThroughEnvelope() throws Exception {
        // 两种失败通道都要稳住: 参数绑定失败 -> 真 4xx; 业务失败 -> HTTP 200 + success:false.
        // 唯一不可接受的是 5xx 或把栈当 HTML 吐出去.
        int http = mockMvc.perform(MockMvcRequestBuilders.post("/api/lc/runtime/list")
                        .param("entityCode", "nope_xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":1,\"size\":10}"))
                .andReturn().getResponse().getStatus();
        assertTrue(http < 500, "unknown entity must never surface as 5xx, got " + http);

        JsonNode node = post("/api/lc/runtime/list", "{\"page\":1,\"size\":10}", "entityCode", "nope_xyz");
        assertFalse(node.path("success").asBoolean(), "unknown entity must fail: " + node);
    }

    @Test
    @DisplayName("应用生命周期 create/detail/publish/archive 全程走信封")
    void appLifecycleOverHttp() throws Exception {
        String code = "it_" + System.nanoTime();
        JsonNode created = post("/api/lc/app/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + code + "\",\"appName\":\"IT App\"}");
        assertOk(created, "create app");
        assertEquals(code, created.path("data").path("appCode").asText());

        JsonNode detail = get("/api/lc/app/detail", "appCode", code);
        assertOk(detail, "app detail");
        assertEquals("IT App", detail.path("data").path("appName").asText());

        assertOk(post("/api/lc/app/publish", "{\"appCode\":\"" + code + "\"}"), "publish");
        assertOk(post("/api/lc/app/archive", "{\"appCode\":\"" + code + "\"}"), "archive");
    }

    @Test
    @DisplayName("事件链: 能取到头节点，且因果冲突不得被包成 success:true")
    void eventChainHeadAndHonestConflict() throws Exception {
        JsonNode head = get("/api/lc/app/event/last", "appCode", APP, "tenantCode", TENANT);
        assertOk(head, "event/last");
        String parent = head.path("data").path("eventId").asText("");
        assertFalse(parent.isEmpty(), "seeded app should expose its chain head");

        // 正确的 parentEventId -> 追加成功
        JsonNode good = post("/api/lc/app/event",
                "{\"tenantCode\":\"" + TENANT + "\",\"entityCode\":\"" + ENTITY + "\",\"eventType\":\"UPDATE\","
                        + "\"eventData\":\"{\\\"description\\\":\\\"V2\\\"}\",\"source\":\"it\",\"parentEventId\":\"" + parent + "\"}",
                "appCode", APP);
        assertOk(good, "append with correct parent");

        // 复用同一个 parent -> 因果冲突. 历史上这里返回 Result.success(Result.fail(409)),
        // 外层 success=true, 任何只看外层的客户端都会把失败写入当成成功.
        JsonNode stale = post("/api/lc/app/event",
                "{\"tenantCode\":\"" + TENANT + "\",\"entityCode\":\"" + ENTITY + "\",\"eventType\":\"UPDATE\","
                        + "\"eventData\":\"{\\\"description\\\":\\\"V3\\\"}\",\"source\":\"it\",\"parentEventId\":\"" + parent + "\"}",
                "appCode", APP);
        assertFalse(stale.path("success").asBoolean(),
                "a causal conflict must surface as success:false, got " + stale);
        assertEquals(409, stale.path("code").asInt(), "conflict must keep its 409 code: " + stale);
    }

    @Test
    @DisplayName("服务端 undo/redo：改坏一行能撤回前像，redo 再推回去")
    void undoAndRedoRestoreRowImages() throws Exception {
        // 不假设种子值、也不假设执行顺序: 先读当前值当基线, 改走, 再撤回基线.
        String original = orderName(1);
        assertOk(updateName(1, "TEMP-" + System.nanoTime()), "update name");
        String changed = orderName(1);
        assertNotEquals(original, changed, "update should have landed");

        JsonNode undone = post("/api/lc/undo/undo", body(), "entityCode", ENTITY);
        assertOk(undone, "undo");
        assertTrue(undone.path("data").path("applied").asBoolean(), "undo should apply: " + undone);
        assertEquals("UPDATE", undone.path("data").path("operation").asText());
        assertEquals(original, orderName(1), "undo must restore the recorded before-image");

        JsonNode redone = post("/api/lc/undo/redo", body(), "entityCode", ENTITY);
        assertOk(redone, "redo");
        assertEquals(changed, orderName(1), "redo must re-apply the recorded after-image");

        // 还原, 别把改动留给其它用例
        post("/api/lc/undo/undo", body(), "entityCode", ENTITY);
        assertEquals(original, orderName(1), "teardown should restore the baseline");
    }

    @Test
    @DisplayName("连续 undo 按 LIFO 逐条回退，撤销过的不会再被撤销")
    void undoWalksBackLifoAndStops() throws Exception {
        String original = orderName(2);
        assertOk(updateName(2, "STEP-A"), "first update");
        assertOk(updateName(2, "STEP-B"), "second update");
        assertEquals("STEP-B", orderName(2));

        assertOk(post("/api/lc/undo/undo", body(), "entityCode", ENTITY), "undo #1");
        assertEquals("STEP-A", orderName(2), "undo #1 should land on the previous image, not skip back");
        assertOk(post("/api/lc/undo/undo", body(), "entityCode", ENTITY), "undo #2");
        assertEquals(original, orderName(2), "undo #2 should reach the baseline");

        // 两条 UPDATE 都已 undone_by 非空, 再撤销要么无目标、要么只能撤到 CREATE —— 绝不能把已撤的再撤一遍
        JsonNode third = post("/api/lc/undo/undo", body(), "entityCode", ENTITY);
        assertTrue(third.path("success").asBoolean(), String.valueOf(third));
        assertEquals(original, orderName(2), "an already-undone change must not be undone twice");
    }

    @Test
    @DisplayName("变更历史可读，且快照里不含 *_label 派生列")
    void changeHistoryExposesImagesWithoutDerivedColumns() throws Exception {
        String original = orderName(1);
        assertOk(updateName(1, "HIST"), "update for history probe");
        JsonNode entries = D(get("/api/lc/undo/history", "appCode", APP, "entityCode", ENTITY, "tenantCode", TENANT));
        assertTrue(entries.isArray() && entries.size() >= 1, "history should carry changes: " + entries);
        boolean sawUpdate = false;
        for (JsonNode entry : entries) {
            for (String key : new String[]{"beforeImage", "afterImage"}) {
                String image = entry.path(key).asText("");
                assertFalse(image.contains("_label"),
                        "snapshot must not carry JOIN-derived *_label columns (" + key + "): " + image);
            }
            if ("UPDATE".equals(entry.path("operation").asText())) {
                sawUpdate = true;
            }
        }
        assertTrue(sawUpdate, "an UPDATE entry should exist: " + entries);
        post("/api/lc/undo/undo", body(), "entityCode", ENTITY);
        assertEquals(original, orderName(1), "teardown should restore the baseline");
    }

    private static String body() {
        return "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}";
    }

    private JsonNode updateName(long id, String name) throws Exception {
        return post("/api/lc/runtime/update",
                "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                        + "\"fieldValues\":{\"id\":" + id + ",\"name\":\"" + name + "\"}}",
                "entityCode", ENTITY);
    }

    private String orderValue(long id, String column) throws Exception {
        JsonNode row = D(call(MockMvcRequestBuilders.post("/api/lc/runtime/get")
                .param("entityCode", ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP
                        + "\",\"tenantCode\":\"" + TENANT + "\",\"id\":" + id + "}")));
        return row == null ? null : row.path(column).asText(null);
    }

    private String orderName(long id) throws Exception {
        return orderNameById(id);
    }

    private String orderNameById(long id) throws Exception {
        return orderValue(id, "name");
    }

    private static JsonNode D(JsonNode envelope) {
        return envelope.path("data");
    }

    @Test
    @DisplayName("undo 是 per-user 的：B 撤销不会把 A 刚写的改动回滚掉")
    void undoIsScopedPerActor() throws Exception {
        String baseline = orderName(1);

        // A 改
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lc/runtime/update").param("entityCode", ENTITY)
                        .header("X-User-Code", "alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP
                                + "\",\"tenantCode\":\"" + TENANT + "\",\"fieldValues\":{\"id\":1,\"name\":\"AlicesEdit\"}}"))
                .andReturn();
        assertEquals("AlicesEdit", orderName(1), "alice's update should land");

        // B 撤销 -> 不应命中 alice 的那条
        JsonNode bobUndo = postAs("bob", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        assertOk(bobUndo, "bob undo");
        assertFalse(bobUndo.path("data").path("applied").asBoolean(),
                "bob must not undo alice's change: " + bobUndo);
        assertEquals("AlicesEdit", orderName(1), "alice's data must survive bob's undo attempt");

        // A 撤销 -> 命中自己的栈
        JsonNode aliceUndo = postAs("alice", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        assertOk(aliceUndo, "alice undo");
        assertTrue(aliceUndo.path("data").path("applied").asBoolean(), "alice should undo her own change: " + aliceUndo);
        assertEquals(baseline, orderName(1), "alice's undo restores the before-image");
    }

    @Test
    @DisplayName("写路径把 actor 记进变更日志，历史按人可筛")
    void journalRecordsActorAndHistoryFiltersByOwner() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lc/runtime/update").param("entityCode", ENTITY)
                        .header("X-User-Code", "carol").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP
                                + "\",\"tenantCode\":\"" + TENANT + "\",\"fieldValues\":{\"id\":1,\"name\":\"CarolsEdit\"}}"))
                .andReturn();

        // owner=mine 认的是当前请求头身份
        JsonNode mine = asArray("carol", "/api/lc/undo/history", "appCode", APP,
                "entityCode", ENTITY, "tenantCode", TENANT, "owner", "mine");
        assertTrue(mine.isArray(), "history should be an array: " + mine);
        for (JsonNode entry : mine) {
            assertEquals("carol", entry.path("actor").asText(),
                    "owner=mine must filter to the caller: " + entry);
        }

        JsonNode all = D(get("/api/lc/undo/history", "appCode", APP, "entityCode", ENTITY,
                "tenantCode", TENANT, "owner", "all"));
        boolean sawCarol = false;
        for (JsonNode entry : all) {
            if ("carol".equals(entry.path("actor").asText())) {
                sawCarol = true;
            }
        }
        assertTrue(sawCarol, "the journal must attribute the write to carol: " + all);
        postAs("carol", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
    }

    private JsonNode asArray(String actor, String path, String... kv) {
        try {
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                            .header("X-User-Code", actor);
            for (int i = 0; i + 1 < kv.length; i += 2) {
                builder = builder.param(kv[i], kv[i + 1]);
            }
            return json.readTree(mockMvc.perform(builder).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JsonNode postAs(String actor, String path, String body) {
        try {
            String raw = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post(path).contentType(MediaType.APPLICATION_JSON).content(body)
                            .characterEncoding(StandardCharsets.UTF_8.name())
                            .param("entityCode", ENTITY)
                            .param("appCode", APP)
                            .param("tenantCode", TENANT)
                            .header("X-User-Code", actor))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            return json.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @DisplayName("分组聚合：按字典字段分组要带可读标签，数值聚合要算对，筛选要生效")
    void aggregateGroupsByDictFieldWithLabels() throws Exception {
        JsonNode groupsResp = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"status\",\"aggregations\":{\"amount\":[\"SUM\",\"MAX\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(groupsResp, "aggregate group by status");
        JsonNode groups = D(groupsResp);
        assertTrue(groups.isArray(), "aggregate should return an array: " + groups);
        java.util.Map<String, JsonNode> byKey = new java.util.HashMap<String, JsonNode>();
        for (JsonNode row : groups) {
            byKey.put(row.path("group_key").asText(), row);
        }
        assertTrue(byKey.containsKey("OK") && byKey.containsKey("PENDING"),
                "seed has both statuses, got " + byKey.keySet());
        assertEquals("Done", byKey.get("OK").path("group_label").asText(),
                "dict-backed group must carry the human label");
        assertEquals(100.0, byKey.get("OK").path("sum_amount").asDouble(), 0.001);
        assertEquals(100.0, byKey.get("OK").path("max_amount").asDouble(), 0.001);
        assertEquals(50.0, byKey.get("PENDING").path("sum_amount").asDouble(), 0.001);

        JsonNode filteredResp = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"status\",\"filters\":{\"amount:gte\":60}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(filteredResp, "aggregate with filter");
        JsonNode filtered = D(filteredResp);
        assertTrue(filtered.isArray() && filtered.size() == 1,
                "filter should leave only the OK bucket: " + filtered);
        assertEquals("OK", filtered.get(0).path("group_key").asText());

        JsonNode totalResp = post("/api/lc/runtime/aggregate", "{}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(totalResp, "total aggregate");
        JsonNode total = D(totalResp);
        assertTrue(total.isArray() && total.size() == 1, "no groupField should yield one total row: " + total);
        assertTrue(total.get(0).path("group_count").asLong() >= 2, "total row should count the seed: " + total);
    }

    @Test
    @DisplayName("聚合入参走白名单：注入串、非数值列、未知函数都必须 400 而不是静默降级")
    void aggregateRejectsIllegalColumns() throws Exception {
        JsonNode injection = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"name; DROP TABLE z_lc_app\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(injection.path("success").asBoolean(), "injected groupField must fail: " + injection);
        assertEquals(400, injection.path("code").asInt(), String.valueOf(injection));

        JsonNode unknown = post("/api/lc/runtime/aggregate", "{\"groupField\":\"nope\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(unknown.path("success").asBoolean(), "unknown groupField must fail, not fall back to 全量");

        JsonNode textAgg = post("/api/lc/runtime/aggregate",
                "{\"aggregations\":{\"name\":[\"SUM\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(textAgg.path("success").asBoolean(), "SUM on a text column must be rejected");

        JsonNode badFn = post("/api/lc/runtime/aggregate",
                "{\"aggregations\":{\"amount\":[\"STDDEV\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(badFn.path("success").asBoolean(), "only whitelisted functions are allowed");

        // 表还活着
        assertOk(get("/api/lc/app/list"), "app list survives the aggregate probes");

    }

    @Test
    @DisplayName("整形：二维聚合结果抬成视图要的高维结构，程序写错必须 400 而不是回一份空结构")
    void shapeTurnsAggregateRowsIntoAViewDocument() throws Exception {
        // 看板最常见的一个形状: 以状态码为键的文档, 前端取值不用再自己 indexBy 一遍
        JsonNode keyed = post("/api/lc/runtime/shape",
                "{\"groupField\":\"status\",\"aggregations\":{\"amount\":[\"SUM\"]},\"shape\":["
                        + "{\"op\":\"select\",\"fields\":{\"status\":\"group_key\","
                        + "\"label\":\"group_label\",\"amount\":\"sum_amount\"}},"
                        + "{\"op\":\"keyBy\",\"key\":\"status\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(keyed, "shape aggregate rows into a keyed document");
        JsonNode data = D(keyed);
        assertTrue(data.isObject() && !data.isArray(), "keyBy 的产出必须是对象, 还是数组说明程序没生效: " + data);
        assertEquals("Done", data.path("OK").path("label").asText(), String.valueOf(data));
        assertEquals(100.0, data.path("OK").path("amount").asDouble(), 0.001);

        // 同一份二维结果, 换一个程序就是另一个形状: 排序与截断都发生在整形里, 不额外发 SQL
        JsonNode top = post("/api/lc/runtime/shape",
                "{\"groupField\":\"status\",\"aggregations\":{\"amount\":[\"SUM\"]},\"shape\":["
                        + "{\"op\":\"select\",\"fields\":{\"status\":\"group_key\",\"amount\":\"sum_amount\"}},"
                        + "{\"op\":\"order\",\"by\":[\"amount desc\"]}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(top, "shape a sorted document");
        JsonNode sorted = D(top);
        assertTrue(sorted.isArray() && sorted.size() >= 2, "种子至少有两档状态: " + sorted);
        double previous = Double.MAX_VALUE;
        for (JsonNode row : sorted) {
            // 种子里有一档记录没有金额 (sum 为 null); desc 必须把它留在末尾,
            // 否则"取金额最高的一档"会取回一个空桶
            assertTrue(row.path("amount").isNull() || row.path("amount").asDouble() <= previous,
                    "desc 必须单调不增, 且空值不参与翻转: " + sorted);
            if (!row.path("amount").isNull()) {
                previous = row.path("amount").asDouble();
            }
        }
        assertTrue(sorted.get(0).path("amount").isNumber(), "top 档不能是空桶: " + sorted);

        JsonNode one = post("/api/lc/runtime/shape",
                "{\"groupField\":\"status\",\"aggregations\":{\"amount\":[\"SUM\"]},\"shape\":["
                        + "{\"op\":\"select\",\"fields\":{\"status\":\"group_key\",\"amount\":\"sum_amount\"}},"
                        + "{\"op\":\"order\",\"by\":[\"amount desc\"]},{\"op\":\"limit\",\"n\":1}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(one, "shape a top-1 document");
        JsonNode topRows = D(one);
        assertTrue(topRows.isArray() && topRows.size() == 1, "limit 1 应只剩一档: " + topRows);
        assertEquals(sorted.get(0).path("status").asText(), topRows.get(0).path("status").asText(),
                "limit 取的就是排序后的头一档");

        JsonNode badOp = post("/api/lc/runtime/shape",
                "{\"groupField\":\"status\",\"shape\":[{\"op\":\"sort\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(badOp.path("success").asBoolean(), "未知步骤必须报错: " + badOp);
        assertEquals(400, badOp.path("code").asInt(), String.valueOf(badOp));
        assertTrue(badOp.path("message").asText().contains("对象语言可用步骤"),
                "报错要自解释 (给出可用步骤清单), 配置的人才改得动: " + badOp);

        JsonNode noShape = post("/api/lc/runtime/shape", "{\"groupField\":\"status\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(noShape.path("success").asBoolean(), "缺 shape 不能悄悄退化成裸聚合: " + noShape);
        assertEquals(400, noShape.path("code").asInt(), String.valueOf(noShape));
    }

    @Test
    @DisplayName("时间分桶聚合：按月/日分桶要真在 H2 上跑得动、按时间正序，且没有日期的记录不进气泡")
    void timeBucketAggregateRunsOnRealDates() throws Exception {
        JsonNode monthResp = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"due_date\",\"timeGroup\":\"MONTH\",\"aggregations\":{\"amount\":[\"SUM\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(monthResp, "aggregate month buckets");
        JsonNode months = D(monthResp);
        assertTrue(months.isArray() && months.size() >= 2, "seed spans two months: " + months);

        java.util.Map<String, JsonNode> byBucket = new java.util.LinkedHashMap<String, JsonNode>();
        int previous = -1;
        for (JsonNode row : months) {
            String key = row.path("bucket_year").asInt() + "-" + row.path("bucket_month").asInt();
            byBucket.put(key, row);
            int month = row.path("bucket_month").asInt();
            assertTrue(month >= previous, "时间轴必须正序 (逆序会把折线图画成锯齿): " + months);
            previous = month;
            assertFalse(row.has("bucket_day"), "MONTH 粒度不该冒出 bucket_day: " + row);
        }
        assertEquals("2026-1", byBucket.keySet().iterator().next(), String.valueOf(byBucket.keySet()));
        assertEquals(100.0, byBucket.get("2026-1").path("sum_amount").asDouble(), 0.001);
        assertEquals(50.0, byBucket.get("2026-2").path("sum_amount").asDouble(), 0.001);
        assertEquals(1, byBucket.get("2026-1").path("group_count").asInt());

        JsonNode dayResp = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"closed_at\",\"timeGroup\":\"DAY\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(dayResp, "aggregate day buckets over a DATETIME column");
        boolean sawDay = false;
        for (JsonNode row : D(dayResp)) {
            if (row.path("bucket_year").asInt() == 2026 && row.path("bucket_month").asInt() == 1
                    && row.path("bucket_day").asInt() == 15) {
                sawDay = true;
            }
        }
        assertTrue(sawDay, "DATETIME 列要能分到日, got " + D(dayResp));

        // 没有日期的记录进不了时间轴 —— 但它也不能把总数算进来 (口径要自洽)
        long dated = 0;
        for (JsonNode row : months) {
            dated += row.path("group_count").asLong();
        }
        jdbc().update("INSERT INTO lc_demo_order (tenant_code, name, amount, status, due_date, deleted)"
                + " VALUES ('default', 'NoDate', 7.00, 'OK', NULL, 0)");
        try {
            JsonNode after = post("/api/lc/runtime/aggregate",
                    "{\"groupField\":\"due_date\",\"timeGroup\":\"MONTH\"}",
                    "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
            assertOk(after, "aggregate after inserting an undated row");
            long datedAfter = 0;
            for (JsonNode row : D(after)) {
                datedAfter += row.path("group_count").asLong();
            }
            assertEquals(Long.valueOf(dated), Long.valueOf(datedAfter),
                    "无日期的记录必须被时间轴排除, 且不影响其他桶");
        } finally {
            jdbc().update("DELETE FROM lc_demo_order WHERE name = 'NoDate'");
        }
    }

    @Test
    @DisplayName("时间分桶入参校验：非日期列、缺分组列、未知粒度都必须 400，不能静默变成全量总数")
    void timeBucketRejectsBadCombinations() throws Exception {
        JsonNode onText = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"status\",\"timeGroup\":\"MONTH\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(onText.path("success").asBoolean(), "按字典列做时间分桶必须报错: " + onText);
        assertEquals(400, onText.path("code").asInt(), String.valueOf(onText));

        JsonNode noGroup = post("/api/lc/runtime/aggregate", "{\"timeGroup\":\"MONTH\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(noGroup.path("success").asBoolean(), "没有 groupField 就无从分桶: " + noGroup);
        assertTrue(noGroup.path("message").asText("").contains("groupField"),
                "error should name the missing argument: " + noGroup);

        JsonNode week = post("/api/lc/runtime/aggregate",
                "{\"groupField\":\"due_date\",\"timeGroup\":\"WEEK\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(week.path("success").asBoolean(),
                "WEEK 没有跨数据库可移植写法, 不能假装支持: " + week);

        // 静默降级的反面证据: 报错之后按原样查月度还要是对的
        assertOk(post("/api/lc/runtime/aggregate", "{\"groupField\":\"due_date\",\"timeGroup\":\"MONTH\"}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT), "month buckets still serve");
    }


    @Test
    @DisplayName("部分更新不能被必填校验挡掉：未提交的列以库里现值参与判定")
    void partialUpdatePassesRequiredCheckAgainstExistingRow() throws Exception {
        // 本用例会写种子行, 结束后必须还原, 否则 filterOperatorsWork / aggregate 会看到被改脏的数据
        final String originalStatus = orderValue(1, "status");
        try {
            doPartialUpdateAssertions();
        } finally {
            post("/api/lc/runtime/update",
                    "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                            + "\"fieldValues\":{\"id\":1,\"status\":\"" + originalStatus + "\"}}",
                    "entityCode", ENTITY);
            assertEquals(originalStatus, orderValue(1, "status"), "seed row must be handed back clean");
        }
    }

    private void doPartialUpdateAssertions() throws Exception {
        // `name` 在种子里标了 required，而这次只提交 status —— 旧实现会直接 400
        JsonNode partial = post("/api/lc/runtime/update",
                "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                        + "\"fieldValues\":{\"id\":1,\"status\":\"PENDING\"}}",
                "entityCode", ENTITY);
        assertOk(partial, "partial update should pass required check");
        assertEquals(1, partial.path("data").asInt(), "one row should have been touched: " + partial);
        assertEquals("Alice", orderName(1), "the untouched required column must keep its value");

        // 但用户显式清空必填列，仍然必须被拒
        JsonNode cleared = post("/api/lc/runtime/update",
                "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                        + "\"fieldValues\":{\"id\":1,\"name\":\"\"}}",
                "entityCode", ENTITY);
        assertFalse(cleared.path("success").asBoolean(),
                "explicitly blanking a required column must still fail: " + cleared);
        assertEquals(400, cleared.path("code").asInt(), String.valueOf(cleared));

        // 新建时漏掉必填列也照旧拒绝
        JsonNode badCreate = post("/api/lc/runtime/create",
                "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                        + "\"fieldValues\":{\"status\":\"OK\"}}",
                "entityCode", ENTITY);
        assertFalse(badCreate.path("success").asBoolean(), "create without a required column must fail");

        // existingValues 是服务端内部字段，客户端塞了也不能生效
        JsonNode spoof = post("/api/lc/runtime/update",
                "{\"entityCode\":\"" + ENTITY + "\",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\","
                        + "\"existingValues\":{\"name\":\"客户端伪造\"},\"fieldValues\":{\"id\":1,\"status\":\"OK\"}}",
                "entityCode", ENTITY);
        assertOk(spoof, "spoofed existingValues must not break the update path");
        assertEquals("Alice", orderName(1), "spoofed pre-image must not be written or trusted blindly");
    }

    /** 数一下当前有多少行，用来证明"整批未写入"是真的没写。 */
    private long rowCount() throws Exception {
        return D(post("/api/lc/runtime/list", "{\"page\":1,\"size\":1}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT)).path("total").asLong();
    }

    @Test
    @DisplayName("批量导入 preview 只校验零写入，且与单条 create 同一口径")
    void importPreviewValidatesWithoutWriting() throws Exception {
        long before = rowCount();
        JsonNode previewResp = post("/api/lc/runtime/import/preview",
                "{\"records\":[{\"name\":\"好的一行\",\"status\":\"OK\"},"
                        + "{\"name\":\"分数写错\",\"amount\":\"abc\"},"
                        + "{\"amount\":5}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertOk(previewResp, "preview");
        JsonNode bad = D(previewResp);
        assertEquals(3, bad.path("total").asInt(), String.valueOf(bad));
        assertEquals(1, bad.path("validCount").asInt(), "只有第一行合法: " + bad);
        assertFalse(bad.path("applied").asBoolean(), "preview 绝不能写");
        assertEquals(0, bad.path("insertedCount").asInt());
        java.util.Set<String> messages = new java.util.HashSet<String>();
        for (JsonNode err : bad.path("errors")) {
            messages.add(err.path("message").asText());
            assertTrue(err.path("index").asInt() >= 0 && err.path("index").asInt() < 3,
                    "行号必须能对上: " + err);
        }
        assertTrue(joined(messages).contains("RequiredCheck") || joined(messages).contains("必填"),
                "必填错误要带字段名: " + messages);
        assertEquals(before, rowCount(), "preview 之后行数不能变");
    }

    private static String joined(java.util.Collection<String> in) {
        StringBuilder sb = new StringBuilder();
        for (String s : in) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("批量导入 commit：有一行坏就整批不写；全好则一次写完并可撤销")
    void importCommitIsAllOrNothingAndUndoable() throws Exception {
        long before = rowCount();
        JsonNode partialBad = D(post("/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"能过\"},{\"amount\":\"x\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertFalse(partialBad.path("applied").asBoolean(), "有一行不合法时整批不该写入: " + partialBad);
        assertEquals(0, partialBad.path("insertedCount").asInt());
        assertEquals(before, rowCount(), "失败的那批不能留下半截数据");

        // 用 alice 的身份提交，下面才用 alice 撤销 —— 导入的行同样按人归栈，
        // 不带 X-User-Code 的批量写入会归到 anonymous，别人撤不动是预期行为。
        JsonNode cleanResp = postAs("alice", "/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"批量甲\",\"amount\":1,\"status\":\"OK\"},"
                        + "{\"name\":\"批量乙\",\"amount\":2,\"status\":\"PENDING\"}]}");
        assertOk(cleanResp, "clean commit");
        JsonNode clean = D(cleanResp);
        assertTrue(clean.path("applied").asBoolean(), "全合法就该写入: " + clean);
        assertEquals(2, clean.path("insertedCount").asInt());
        assertEquals(2, clean.path("ids").size(), "要把插入的 id 回传给调用方");
        assertEquals(before + 2, rowCount());

        // 批量写入不是撤销的黑洞：每一行都进了变更日志
        JsonNode undone = postAs("alice", "/api/lc/undo/undo",
                "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        assertOk(undone, "undo an imported row");
        assertTrue(undone.path("data").path("applied").asBoolean(), "导入的行应可撤销: " + undone);
        assertEquals("CREATE", undone.path("data").path("operation").asText());
        assertEquals(before + 1, rowCount(), "撤销一条后剩一条");
        postAs("alice", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        assertEquals(before, rowCount(), "再撤销一次应回到原状");
    }

    @Test
    @DisplayName("批量导入守行数上限，不给一个请求把库打挂的机会")
    void importRejectsOversizedBatch() throws Exception {
        StringBuilder records = new StringBuilder();
        for (int i = 0; i <= 2000; i++) {
            if (i > 0) {
                records.append(',');
            }
            records.append("{\"name\":\"row").append(i).append("\"}");
        }
        JsonNode tooMany = post("/api/lc/runtime/import/commit",
                "{\"records\":[" + records + "]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(tooMany.path("success").asBoolean(), "超过上限必须拒绝: " + tooMany.path("message").asText());
        assertTrue(tooMany.path("message").asText().contains("分批"), "要给出可操作的提示: " + tooMany);
    }

    /** 用批量导入拿到一批干净的 id：返回去重后的 [X, Y] 两条新建记录 id。 */
    private List<Long> seedTwoRows(String actor) throws Exception {
        JsonNode committed = D(postAs(actor, "/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"批删甲\",\"status\":\"OK\"},{\"name\":\"批删乙\",\"status\":\"PENDING\"}]}"));
        assertTrue(committed.path("applied").asBoolean(), "seed 批次应当写成功: " + committed);
        List<Long> ids = new ArrayList<>();
        for (JsonNode id : committed.path("ids")) {
            ids.add(id.asLong());
        }
        assertEquals(2, ids.size(), "seed 需要两个 id, got " + committed);
        return ids;
    }

    @Test
    @DisplayName("批量删除：有一个 id 不能删就整批不删；去重后一条请求删完整批并可撤销")
    void batchDeleteIsAllOrNothingAndUndoable() throws Exception {
        long before = rowCount();
        List<Long> ids = seedTwoRows("bd_keep");
        Long x = ids.get(0);
        Long y = ids.get(1);
        assertEquals(before + 2, rowCount());

        // ① 掺一个不存在的 id：整批不删。这是这个端点存在的理由 —— 旧的逐行 delete 会先把前面的删掉。
        JsonNode blocked = D(postAs("bd_keep", "/api/lc/runtime/delete-batch",
                "{\"ids\":[" + x + "," + y + ",999999999]}"));
        assertFalse(blocked.path("applied").asBoolean(), "坏 id 必须让整批不删: " + blocked);
        assertEquals(0, blocked.path("deletedCount").asInt(), "没删成就不该报删除数: " + blocked);
        assertEquals(1, blocked.path("errors").size(), "要指出是哪一条: " + blocked);
        assertEquals(999999999L, blocked.path("errors").get(0).path("id").asLong(), String.valueOf(blocked));
        assertTrue(blocked.path("message").asText().contains("整批未删除"), "要说清什么都没删: " + blocked);
        assertEquals(before + 2, rowCount(), "失败的那批不能留下半删现场");
        assertNotNull(orderName(x), "预检失败时这条不该被动过");
        assertNotNull(orderName(y), "预检失败时这条不该被动过");

        // ② 重复 id 不该把整批拖进回滚：去重后一次删完，报的条数是去重后的条数。
        JsonNode ok = D(postAs("bd_keep", "/api/lc/runtime/delete-batch",
                "{\"ids\":[" + x + "," + x + "," + y + "]}"));
        assertTrue(ok.path("applied").asBoolean(), "全是好 id 就该删成功: " + ok);
        assertEquals(2, ok.path("total").asInt(), "重复 id 要去重后再计数: " + ok);
        assertEquals(2, ok.path("deletedCount").asInt(), String.valueOf(ok));
        assertEquals(2, ok.path("ids").size(), "要把真正删掉的 id 回传");
        assertEquals(before, rowCount(), "两条都该消失");
        assertNullSafe(orderName(x), "软删后按 id 读不到");
        assertNullSafe(orderName(y), "软删后按 id 读不到");

        // ③ 批量删除不是撤销的黑洞：两条都进了变更日志，各撤一次能全部回来。
        for (int i = 0; i < 2; i++) {
            JsonNode undone = postAs("bd_keep", "/api/lc/undo/undo",
                    "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
            assertTrue(undone.path("data").path("applied").asBoolean(),
                    "批量删掉的记录应当可撤销: " + undone);
            assertEquals("DELETE", undone.path("data").path("operation").asText(), String.valueOf(undone));
        }
        assertEquals(before + 2, rowCount(), "撤销两次后两条都回来");
        // 收尾：把 seed 的两行撤掉，别污染别的用例（撤 DELETE 之后栈上还剩 CREATE，撤 CREATE 即软删）
        for (int i = 0; i < 2; i++) {
            postAs("bd_keep", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        }
        assertEquals(before, rowCount(), "用例结束时行数要回到原状");
    }

    private static void assertNullSafe(String value, String context) {
        assertTrue(value == null || value.isEmpty(), context + ", got " + value);
    }

    @Test
    @DisplayName("批量删除的预检认的是活记录：已被删掉的 id 会让整批停下，不牵连其他行")
    void batchDeleteRejectsAlreadyDeletedId() throws Exception {
        long before = rowCount();
        List<Long> ids = seedTwoRows("bd_race");
        Long x = ids.get(0);
        Long y = ids.get(1);

        // 单删走的是另一个入口，也用它来构造"预检之后、写入之前被别人删掉"这个现场。
        // 这里必须带自己的 X-User-Code：用共享身份会把日志挂到 anonymous 栈上，
        // 别的用例撤 anonymous 时会撤到我这条，跨用例串台。
        // ⚠ /runtime/delete 只认 body 里的 appCode/tenantCode（query 参数它不读），
        //    和 /runtime/list、/runtime/delete-batch 的口径不一样，所以这里两个都塞。
        JsonNode single = postAs("bd_race", "/api/lc/runtime/delete",
                "{\"id\":" + x + ",\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
        assertEquals(1, single.path("data").asInt(), "单条软删应当生效: " + single);
        assertEquals(before + 1, rowCount());

        JsonNode blocked = D(postAs("bd_race", "/api/lc/runtime/delete-batch",
                "{\"ids\":[" + x + "," + y + "]}"));
        assertFalse(blocked.path("applied").asBoolean(), "x 已经不存在，整批不该动: " + blocked);
        assertEquals(0, blocked.path("deletedCount").asInt());
        assertEquals(x, Long.valueOf(blocked.path("errors").get(0).path("id").asLong()));
        assertEquals(before + 1, rowCount(), "y 不能被顺手删掉，也不能被删两次");
        assertNotNull(orderName(y), "y 应当完好");

        JsonNode second = D(postAs("bd_race", "/api/lc/runtime/delete-batch",
                "{\"ids\":[" + y + "]}"));
        assertTrue(second.path("applied").asBoolean(), "只剩好 id 时应当删成功: " + second);
        assertEquals(1, second.path("deletedCount").asInt());
        assertEquals(before, rowCount(), "本用例造的现场收尾时应当全部软删");
    }

    @Test
    @DisplayName("批量删除守条数上限，不给一个请求把库打挂的机会")
    void batchDeleteRejectsOversizedBatch() throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i <= 2000; i++) {
            if (i > 0) {
                ids.append(',');
            }
            ids.append(1_000_000 + i);
        }
        JsonNode tooMany = post("/api/lc/runtime/delete-batch", "{\"ids\":[" + ids + "]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(tooMany.path("success").asBoolean(), "超过上限必须拒绝: " + tooMany.path("message").asText());
        assertTrue(tooMany.path("message").asText().contains("分批"), "要给出可操作的提示: " + tooMany);
        long after = rowCount();
        JsonNode empty = post("/api/lc/runtime/delete-batch", "{\"ids\":[]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(empty.path("success").asBoolean(), "空批次该拒, 不该假装成功: " + empty);
        assertEquals(after, rowCount(), "被拒的请求一行都不该动");
    }

    @Test
    @DisplayName("聚合分两档：文本列可算去重数/非空数，但 SUM 仍然被拒")
    void aggregateAllowsUniversalFunctionsOnly() throws Exception {
        JsonNode ok = D(post("/api/lc/runtime/aggregate",
                "{\"aggregations\":{\"name\":[\"DISTINCT\",\"FILLED\"],\"status\":[\"DISTINCT\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertOkName(ok, "universal aggregates");
        JsonNode row = ok.get(0);
        assertTrue(row.path("distinct_name").asInt() >= 2,
                "种子有 Alice/Bob 两个不同名字: " + ok);
        assertTrue(row.path("filled_name").asInt() >= 2, "非空计数: " + ok);
        assertTrue(row.path("distinct_status").asInt() >= 1, "字典列也能去重计数: " + ok);

        JsonNode numericOnText = post("/api/lc/runtime/aggregate",
                "{\"aggregations\":{\"name\":[\"SUM\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(numericOnText.path("success").asBoolean(), "文本列 SUM 仍必须被拒");

        JsonNode unknown = post("/api/lc/runtime/aggregate",
                "{\"aggregations\":{\"name\":[\"STDDEV\"]}}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT);
        assertFalse(unknown.path("success").asBoolean(), "函数必须在白名单内");
        assertTrue(unknown.path("message").asText().contains("Unsupported aggregation function"),
                "要先报「不认识的函数」，而不是把它当数值函数去校验列类型: " + unknown.path("message").asText());
    }

    private static void assertOkName(JsonNode arr, String context) {
        assertTrue(arr.isArray(), context + " should be an array");
    }

    @Test
    @DisplayName("字典值不在码表：preview/commit 都给 warning，但不阻断写入")
    void importReportsOutOfRangeDictValuesAsWarnings() throws Exception {
        JsonNode preview = D(post("/api/lc/runtime/import/preview",
                "{\"records\":[{\"name\":\"越界\",\"status\":\"NOT_A_CODE\"},"
                        + "{\"name\":\"正常\",\"status\":\"OK\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertEquals(2, preview.path("validCount").asInt(),
                "越界值不该算校验失败: " + preview);
        assertEquals(0, preview.path("errors").size());
        assertTrue(preview.path("warnings").size() >= 1,
                "应当回一条值域 warning: " + preview);
        JsonNode warning = preview.path("warnings").get(0);
        assertEquals(0, warning.path("index").asInt(), "要指到正确的那一行: " + warning);
        assertEquals("status", warning.path("fieldCode").asText());
        assertTrue(warning.path("message").asText().contains("NOT_A_CODE"),
                "消息里要带上被拒的值: " + warning);

        long before = rowCount();
        JsonNode commit = D(post("/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"越界写入\",\"status\":\"NOPE\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertTrue(commit.path("applied").asBoolean(), "warning 不阻断写入: " + commit);
        assertTrue(commit.path("warnings").size() >= 1, "commit 也要把 warning 带回来");
        assertEquals(before + 1, rowCount());
        // 收尾：撤掉这一行，别污染别的用例
        postAs("alice", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
    }

    @Test
    @DisplayName("preview 不许骗人：超长值必须在预检就被拦下，而不是到库里才炸")
    void previewCatchesLengthViolationsBeforeTheDatabaseDoes() throws Exception {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            tooLong.append('X');
        }
        JsonNode preview = D(post("/api/lc/runtime/import/preview",
                "{\"records\":[{\"name\":\"正常\"},{\"name\":\"" + tooLong + "\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertEquals(1, preview.path("validCount").asInt(),
                "超长值必须在 preview 就算校验失败: " + preview);
        assertTrue(preview.path("errors").get(0).path("message").asText().contains("8")
                        || preview.path("errors").get(0).path("message").asText().contains("32")
                        || preview.path("errors").get(0).path("message").asText().contains("长度"),
                "错误要指出是长度问题: " + preview.path("errors"));
    }

    @Test
    @DisplayName("失败的批量导入不留幽灵变更日志（日志在另一个数据源，不跟主写入一起回滚）")
    void failedImportLeavesNoOrphanJournalEntries() throws Exception {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            tooLong.append('Y');
        }
        long before = rowCount();
        int logsBefore = historySize();

        JsonNode rejected = D(post("/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"正常一行\"},{\"name\":\"" + tooLong + "\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertFalse(rejected.path("applied").asBoolean(), "整批不该写入: " + rejected);
        assertEquals(before, rowCount(), "行数不能变");
        assertEquals(logsBefore, historySize(),
                "失败批次一行都不该往变更日志里写，否则会留下指向不存在记录的幽灵条目");

        // 反过来确认日志确实会记：成功批次应当多出对应条数
        JsonNode ok = D(post("/api/lc/runtime/import/commit",
                "{\"records\":[{\"name\":\"日志校验甲\"}]}",
                "entityCode", ENTITY, "appCode", APP, "tenantCode", TENANT));
        assertTrue(ok.path("applied").asBoolean(), "这批应当写成功: " + ok);
        assertEquals(logsBefore + 1, historySize(), "成功写入要留下日志");
        postAs("anonymous", "/api/lc/undo/undo", "{\"appCode\":\"" + APP + "\",\"tenantCode\":\"" + TENANT + "\"}");
    }

    private int historySize() throws Exception {
        JsonNode history = D(get("/api/lc/undo/history",
                "appCode", APP, "entityCode", ENTITY, "tenantCode", TENANT, "owner", "all"));
        return history.size();
    }

    /* ------------------------------------------------------------------ */
    /*  逆向映射的连接生命周期（模型设计器「从数据库导入」）                  */
    /* ------------------------------------------------------------------ */

    /**
     * 这条是 Druid 抓现行抓出来的：dev 环境跑浏览器门禁时，日志里出现
     * {@code abandon connection, owner thread: http-nio-18090-exec-1} + 借用栈停在
     * {@code DbTableMapperService.scanTables}，连接被持有 66 秒不还。
     * 原代码是 {@code getDataSource().getConnection().getMetaData()} —— 借了不还是常态，
     * 每点一次「扫描」永久吃掉池里一个连接，池一空整个服务就无声挂起。
     */
    @Test
    @DisplayName("逆向映射必须归还它借走的每一个连接")
    void reverseMappingReturnsEveryConnectionItBorrows() {
        AtomicInteger borrowed = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        DbTableMapperService mapper = new DbTableMapperService(trackingDataSource(borrowed, closed));

        List<EntityDefDTO> tables = mapper.scanTables("z_lc_", null);
        assertFalse(tables.isEmpty(), "扫描要真扫到表，否则「没泄漏」是空跑出来的结论");
        EntityDefDTO one = mapper.mapTable("z_lc_dict", null);
        assertFalse(one.getFields().isEmpty(), "单表映射要读出列，不然等于没走那条路");

        assertTrue(borrowed.get() >= 2, "两个方法各该借一次连接，实际借了 " + borrowed.get());
        assertEquals(borrowed.get(), closed.get(),
                "借走 " + borrowed.get() + " 只还了 " + closed.get() + " —— 连接泄漏，池会被吃空");
    }

    /**
     * 同一件事在真实连接池 + 真实 HTTP 路径上再验一遍：连打 6 次逆向映射接口之后，
     * 上下文里**每一个**池的 active 都不许增长（不假设基线为 0，避免被无关持有者干扰）。
     * 遍历所有 DataSource 是刻意的：服务到底注入的是哪一个池不该由测试猜，猜错就会假绿。
     */
    @Test
    @DisplayName("逆向映射接口不得把连接池越打越满")
    void reverseMappingEndpointsDoNotGrowThePool() throws Exception {
        List<javax.sql.DataSource> pools = new ArrayList<>(allDataSources);
        assertFalse(pools.isEmpty(), "测试上下文里应该有连接池");
        List<Integer> before = new ArrayList<>();
        for (javax.sql.DataSource pool : pools) {
            before.add(activeOf(pool));
        }

        for (int i = 0; i < 3; i++) {
            JsonNode scan = D(get("/api/lc/admin/db/tables", "prefix", "z_lc_"));
            assertTrue(scan.size() > 0, "扫描接口该返回表，第 " + i + " 次: " + scan);
            JsonNode preview = D(get("/api/lc/admin/db/table", "tableName", "z_lc_dict"));
            assertEquals("z_lc_dict", preview.path("tableName").asText(), "单表预览结果不对");
        }

        for (int i = 0; i < pools.size(); i++) {
            int after = activeOf(pools.get(i));
            assertTrue(after <= before.get(i), "池 " + i + " 的 active 从 " + before.get(i)
                    + " 涨到 " + after + " —— 有连接借出去没还");
        }
    }

    /** 只统计 getConnection/close，不改任何行为。 */
    private javax.sql.DataSource trackingDataSource(AtomicInteger borrowed, AtomicInteger closed) {
        ClassLoader cl = getClass().getClassLoader();
        return (javax.sql.DataSource) Proxy.newProxyInstance(cl, new Class<?>[]{javax.sql.DataSource.class},
                (proxy, method, args) -> {
                    Object out = method.invoke(dataSource, args);
                    if (!"getConnection".equals(method.getName()) || !(out instanceof Connection)) {
                        return out;
                    }
                    borrowed.incrementAndGet();
                    Connection conn = (Connection) out;
                    return Proxy.newProxyInstance(cl, new Class<?>[]{Connection.class}, (p2, m2, a2) -> {
                        if ("close".equals(m2.getName())) {
                            closed.incrementAndGet();
                        }
                        return m2.invoke(conn, a2);
                    });
                });
    }

    private int activeOf(javax.sql.DataSource pool) {
        if (pool instanceof com.zaxxer.hikari.HikariDataSource) {
            com.zaxxer.hikari.HikariPoolMXBean bean = ((com.zaxxer.hikari.HikariDataSource) pool)
                    .getHikariPoolMXBean();
            return bean == null ? 0 : bean.getActiveConnections();
        }
        if (pool instanceof com.alibaba.druid.pool.DruidDataSource) {
            return (int) ((com.alibaba.druid.pool.DruidDataSource) pool).getActiveCount();
        }
        throw new IllegalStateException("未知连接池实现，测不了 active: " + pool.getClass().getName());
    }
}
