package com.zifang.z.lc.web.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.core.workflow.WorkflowTriggers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 #61 的契约层：绑定这一行到底换出了什么 —— 真起上下文、真打 HTTP、真发一句到桩、真回读账.
 * <p>
 * 为什么单独开一个类而不是往 {@code LcHttpContractTest} 里加几段：这个上下文要带
 * {@code @DynamicPropertySource}（把 {@code z-lc.adapter.wf.base-url} 指到 OS 分配的桩端口、
 * 把派发超时压到 250ms），那是整套上下文参数，混进去会把 2389 行的既有测试一起换个上下文。
 * <p>
 * 这一层存在的理由（与 #41/#43/#48 同一族）：java 单测里的 68 例证明的是"这些类自己会这么做"，
 * 证明不了"运行时有一个人调它"。#61 的原始形状恰恰是
 * {@code listByEvent} + {@code startProcess} 在生产代码里零调用者 ⇒ 全套 DTO 测试全绿。
 * 所以这里的每一条都走真进程边界：
 * <ol>
 *   <li>绑定写入口 → 运行时写记录 → 桩真的收到那一句 → {@code /fires} 读回 STARTED。</li>
 *   <li>拒绝面逐条 400，并且随后写记录时<b>桩一次都没收到</b>（只测 400 不够：那只能证明拒了，
 *       不能证明"拒的就是没接线的形态"）。</li>
 *   <li>引擎不可用/说不了/回得慢，都不能把用户那条记录带走，但必须留下一行说得清的 FAILED。</li>
 * </ol>
 */
@SpringBootTest(classes = LcTestApplication.class)
@AutoConfigureMockMvc
class WorkflowTriggerContractTest {

    private static final String TENANT = "default";
    private static final String ENTITY = "case";
    private static final String FIRES = "/api/lc/workflow-binding/fires";
    private static final String BINDING_CREATE = "/api/lc/workflow-binding/create";

    /** 静态初始化 ⇒ 端口在 Spring 上下文创建之前就存在，@DynamicPropertySource 才拿得到。 */
    private static final StubWf STUB = newStub();

    private static StubWf newStub() {
        try {
            return new StubWf();
        } catch (IOException ex) {
            throw new IllegalStateException("起不了 z-wf 桩，这一族断言全部无效", ex);
        }
    }

    @DynamicPropertySource
    static void wireTheEngineStub(DynamicPropertyRegistry registry) {
        registry.add("z-lc.adapter.wf.base-url", STUB::baseUrl);
        // 派发超时压到 250ms：一是让"回得慢"这一支测得起，二是这一句本身就是断言 ——
        // 这个键在类注释里宣称可配而实际没有任何地方绑过（契约层的超时那一条会红）。
        registry.add("z-lc.workflow.dispatch-timeout-ms", () -> "250");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private ApplicationContext context;

    private int lastHttpStatus;

    @BeforeEach
    @AfterEach
    void resetStub() {
        STUB.reset();
    }

    @AfterAll
    static void shutdownStub() {
        STUB.close();
    }

    private org.springframework.jdbc.core.JdbcTemplate jdbc() {
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }

    /* ------------------------------------------------------------------ */
    /* 1. 真发一句：绑定 → 写记录 → 桩收到 → 账读得回                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#61 绑定真的换出一次发起：桩收到 z-wf 那一句，/fires 读回 STARTED 和实例 id")
    void bindingActuallyFiresAndTheLedgerReadsBackStarted() throws Exception {
        String app = provisionedApp("fire");
        JsonNode binding = createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\","
                + "\"processDefinitionKey\":\" expense_approval \"}");
        assertOk(binding, "登记一条 AFTER_CREATE 绑定: " + binding.path("message").asText());
        String bindingId = binding.path("data").path("id").asText();

        long recordId = writeRecord(app, "{\"ref\":\"WF-1\"}");
        assertEquals(1, STUB.count(), "桩应该正好收到一句: " + STUB.requests());

        StubWf.Recorded sent = STUB.requests().get(0);
        assertEquals("POST", sent.method);
        assertEquals(StubWf.START_PATH, sent.path,
                "打的必须是 z-wf 真映射的那条路径（少了 /api 或 process 少个 s 都是 404）");
        assertTrue(sent.body.contains("\"processKey\":\"expense_approval\""),
                "DTO 读的键是 processKey，且两端空白要剪掉: " + sent.body);
        assertTrue(sent.body.contains("\"businessKey\":\"" + app + ":" + ENTITY + ":" + recordId + "\""),
                "businessKey 要能定位回这条记录: " + sent.body);
        assertTrue(sent.body.contains("\"title\":\"" + ENTITY + "#" + recordId + "\""),
                "title 缺席时审批中心里那一单没有名字: " + sent.body);
        assertTrue(sent.body.contains("\"ref\":\"WF-1\""),
                "字段值要整份当流程变量带走: " + sent.body);
        assertTrue(sent.body.contains("\"lcRecordId\":" + recordId),
                "变量里要留低代码这一侧的坐标: " + sent.body);

        JsonNode rows = get(FIRES, "appCode", app, "entityCode", ENTITY, "recordId", String.valueOf(recordId));
        assertOk(rows, "回读发起账");
        assertEquals(1, rows.path("data").size(), "一条绑定一次发起，账上应该只有一行: " + rows.path("data"));
        JsonNode fire = rows.path("data").get(0);
        assertEquals("STARTED", fire.path("status").asText(), fire.toString());
        assertEquals("wf-stub-77", fire.path("instanceId").asText(),
                "实例 id 必须是 z-wf data.processInstanceId 那一格，不能是整个 data 的 toString: " + fire);
        assertEquals(bindingId, fire.path("bindingId").asText(), fire.toString());
        assertEquals("AFTER_CREATE", fire.path("triggerEvent").asText(), fire.toString());
        assertEquals("expense_approval", fire.path("processDefinitionKey").asText(), fire.toString());
        assertFalse(fire.path("detail").isTextual() && !fire.path("detail").asText().isEmpty(),
                "成功行不该带失败原因: " + fire);

        // 正向对照：没有绑定的实体写记录，一句都不该发（把"发不发"和"发几条"分开钉）。
        long other = writeRecord(app, "{\"ref\":\"WF-2\"}", false);
        assertEquals(1, STUB.count(), "第二次写是 undo 路径之外的一次普通 create，绑定仍然只有一条: "
                + STUB.requests());
        assertTrue(other > 0);
    }

    /* ------------------------------------------------------------------ */
    /* 2. 拒绝面：400 是表象，"一条都不发"才是牙齿                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#61 兑现不了的触发时机在写入口就被拒，且随后写记录时引擎一次都没收到")
    void unrealizableBindingsAreRefusedAndFireNothing() throws Exception {
        String app = provisionedApp("refuse");

        JsonNode update = refused(BINDING_CREATE, bindingJson(app, "AFTER_UPDATE", "p_one"));
        assertTrue(update.path("message").asText().contains("AFTER_UPDATE"), String.valueOf(update));
        assertTrue(update.path("message").asText().contains("这条记录对应哪个流程实例"),
                "拒绝要点名为什么兑现不了，只说「不支持」等于没说: " + update);

        assertTrue(refused(BINDING_CREATE, bindingJson(app, "AFTER_DELETE", "p_two"))
                .path("message").asText().contains("没有实例账"), "删除后: 没有实例账可言");
        assertTrue(refused(BINDING_CREATE, bindingJson(app, "status_change", "p_three"))
                .path("message").asText().contains("status_change"), "引擎没有状态机事件这一类");
        assertTrue(refused(BINDING_CREATE, bindingJson(app, "BEFORE_CREATE", "p_four"))
                .path("message").asText().contains("流水线"), "写前挂接点属于 #41 那一族");
        assertTrue(refused(BINDING_CREATE, bindingJson(app, "AFTER_CREATE", "   "))
                .path("message").asText().contains("processDefinitionKey"), "空 KEY: " + refused(BINDING_CREATE,
                bindingJson(app, "AFTER_CREATE", "   ")));
        assertTrue(refused(BINDING_CREATE, "{\"appCode\":\"" + app + "\",\"entityCode\":\"" + ENTITY
                        + "\",\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p\",\"autoSubmit\":0}")
                .path("message").asText().contains("自动提单"), "关掉自动提单的绑定没有任何运行时行为");

        // 猎物：同样的字段换成引擎真兑现的那一组，必须能登记（否则上面那 5 个 400 是"闸什么都拒"）。
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_ok\"}"),
                "可兑现的那一条应当登记得下来");
        assertEquals(1, STUB.count(), "登记绑定本身不该发起任何流程（只有写记录才发）: " + STUB.requests());
    }

    @Test
    @DisplayName("#61 重复绑定被拒后，写记录只发一句（拒的是第二行，不是整条链）")
    void duplicateBindingIsRefusedWhileTheFirstOneStillFires() throws Exception {
        String app = provisionedApp("dup");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_once\"}"),
                "第一条绑定登记");
        JsonNode twice = refused(BINDING_CREATE, bindingJson(app, "AFTER_CREATE", "p_once"));
        assertTrue(twice.path("message").asText().contains("已经绑定过"), String.valueOf(twice));

        long recordId = writeRecord(app, "{\"ref\":\"DUP-1\"}");
        assertEquals(1, STUB.count(), "重复绑定没能进去 ⇒ 只该发一句: " + STUB.requests());
        assertEquals("STARTED", fireRow(app, recordId).path("status").asText());
    }

    /* ------------------------------------------------------------------ */
    /* 3. 引擎侧任何意外都不许把用户的写入带走                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#61 引擎拒绝：记录照样写成功，账上留一行 FAILED 并带上引擎那句原话")
    void engineRefusalKeepsTheUsersWriteAndLeavesAFailedRow() throws Exception {
        String app = provisionedApp("reject");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_rej\"}"),
                "绑定登记");
        STUB.rejectWith("流程启动失败: business key 已存在");

        long recordId = writeRecord(app, "{\"ref\":\"RJ-1\"}");
        assertTrue(recordId > 0, "外部引擎说不了，用户这条记录不能不落地");
        JsonNode fire = fireRow(app, recordId);
        assertEquals("FAILED", fire.path("status").asText(), fire.toString());
        assertTrue(fire.path("detail").asText().contains("business key 已存在"),
                "失败原因要把引擎那句带回来，否则排查只能翻日志: " + fire);
        assertEquals(1, STUB.count(), String.valueOf(STUB.requests()));
    }

    @Test
    @DisplayName("#61 引擎回得慢：在配置的 250ms 上被切掉，而不是共享客户端那个 60s 读超时")
    void slowEngineIsCutOffAtTheConfiguredDeadline() throws Exception {
        String app = provisionedApp("slow");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_slow\"}"),
                "绑定登记");
        STUB.delay(6000);

        long began = System.nanoTime();
        long recordId = writeRecord(app, "{\"ref\":\"SL-1\"}");
        long elapsedMs = (System.nanoTime() - began) / 1000000L;

        assertTrue(recordId > 0, "挂死的引擎不能按住用户的新建");
        assertTrue(elapsedMs < 3000, "写入口被按住了 " + elapsedMs + "ms —— 派发没有上限就是在按住 60s");
        JsonNode fire = fireRow(app, recordId);
        assertEquals("FAILED", fire.path("status").asText(), fire.toString());
        // 这一句同时钉住"注释里那个配置项真的绑上了"：绑的是 250，不是默认 3000。
        assertTrue(fire.path("detail").asText().contains("250ms"),
                "超时上限要走 z-lc.workflow.dispatch-timeout-ms（这个键此前只是写在注释里，没人绑）: "
                        + fire);
    }

    @Test
    @DisplayName("#61 引擎不可达：一条 FAILED 的账，而不是一句 500")
    void unreachableEngineBecomesAFailedRowNotAnError() throws Exception {
        String app = provisionedApp("down");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_down\"}"),
                "绑定登记");
        STUB.silent();

        long recordId = writeRecord(app, "{\"ref\":\"DN-1\"}");
        assertTrue(recordId > 0);
        JsonNode fire = fireRow(app, recordId);
        assertEquals("FAILED", fire.path("status").asText(), fire.toString());
        assertFalse(fire.path("detail").asText().isEmpty(), "至少要说出为什么: " + fire);
    }

    /* ------------------------------------------------------------------ */
    /* 4. 边界：别人的租户、批量导入、重做                                    */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#61 别的租户的绑定不发这里的记录（真往库里种一行 foreign，而不是只在测试里造对象）")
    void foreignTenantBindingNeverFires() throws Exception {
        String app = provisionedApp("tenant");
        jdbc().update("INSERT INTO z_lc_workflow_binding (tenant_code, app_code, entity_code, trigger_event, "
                        + "process_definition_key, auto_submit, deleted, create_time, update_time) "
                        + "VALUES ('other-tenant', ?, ?, 'AFTER_CREATE', 'p_foreign', 1, 0, NOW(), NOW())",
                app, ENTITY);

        long foreign = writeRecord(app, "{\"ref\":\"T-1\"}");
        assertEquals(0, STUB.count(),
                "外租户的绑定被本租户的记录写触发，等于替别人提单: " + STUB.requests());
        assertEquals(0, get(FIRES, "appCode", app, "recordId", String.valueOf(foreign))
                .path("data").size(), "别人的发起也不该出现在本租户的账里");

        // 阳性对照：同一行只改租户 ⇒ 必须立刻发得出去。少了这一句，上面那个 0 是"闸管用"还是
        // "这条链根本没接"分不开（#48 收口时踩过同一件事）。
        jdbc().update("UPDATE z_lc_workflow_binding SET tenant_code = 'default' "
                + "WHERE app_code = ? AND entity_code = ? AND tenant_code = 'other-tenant'", app, ENTITY);
        long ours = writeRecord(app, "{\"ref\":\"T-2\"}");
        assertEquals(1, STUB.count(), "改掉租户之后发得出去，才证明上面那个 0 是租户过滤给的: "
                + STUB.requests());
        assertEquals("STARTED", fireRow(app, ours).path("status").asText());
    }

    @Test
    @DisplayName("#61 边界如实：批量导入与重做都不发单（这一条钉的是今天的口径，改了就要同时改文档）")
    void importAndRedoDoNotFire() throws Exception {
        String app = provisionedApp("batch");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_batch\"}"),
                "绑定登记");

        // ImportDto.Request 的键是 records（不是 rows），结果里的键是 insertedCount（不是 inserted）。
        JsonNode imported = post("/api/lc/runtime/import/commit",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\",\"records\":["
                        + "{\"ref\":\"IM-1\"},{\"ref\":\"IM-2\"}]}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(imported, "批量导入要成功: " + imported.path("message").asText());
        assertTrue(imported.path("data").path("insertedCount").asInt() >= 1,
                "导入没写进任何行，那这一条什么都没测: " + imported);
        assertEquals(0, STUB.count(),
                "批量导入今天不发单（发就是 N 条记录一次外部调用，且没有回滚路径）: " + STUB.requests());

        long recordId = writeRecord(app, "{\"ref\":\"UD-1\"}");
        assertEquals(1, STUB.count(), "逐条新建要发一句: " + STUB.requests());

        JsonNode undone = post("/api/lc/undo/undo",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\"}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(undone, "撤销刚才那条: " + undone.path("message").asText());
        JsonNode redone = post("/api/lc/undo/redo",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\"}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(redone, "重做刚才那条: " + redone.path("message").asText());
        assertEquals(1, STUB.count(),
                "重做是同一条记录的回放，再发一次就是重复提单（当前口径：不做）: " + STUB.requests());
        assertTrue(recordId > 0);
    }

    /* ------------------------------------------------------------------ */
    /* 5. 词表与结构                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#61 /vocabulary 就是引擎那份清单：界面不必手抄，被拒的每个事件都带原因")
    void vocabularyMirrorsTheEngine() throws Exception {
        JsonNode envelope = get("/api/lc/workflow-binding/vocabulary");
        assertOk(envelope, "读词表");
        JsonNode data = envelope.path("data");

        List<String> implemented = new ArrayList<String>();
        for (JsonNode e : data.path("implemented")) {
            implemented.add(e.asText());
        }
        assertEquals(WorkflowTriggers.implemented(), implemented,
                "词表必须是引擎那份数据本身，而不是手抄的第二份清单");
        assertEquals(Arrays.asList("AFTER_CREATE"), implemented);

        Set<String> rejectedEvents = new LinkedHashSet<String>();
        for (JsonNode e : data.path("rejected")) {
            String event = e.path("event").asText();
            rejectedEvents.add(event);
            assertFalse(e.path("reason").asText().trim().isEmpty(),
                    "被拒清单里只给事件名不给原因，界面上就只能显示「不支持」: " + e);
        }
        // 界面上今天摆着的那两个选项必须在这份"为什么不行"的清单里，否则前端无从显示理由。
        assertTrue(rejectedEvents.containsAll(Arrays.asList("AFTER_UPDATE", "AFTER_DELETE")),
                "界面摆过的事件没进被拒清单: " + rejectedEvents);
    }

    @Test
    @DisplayName("#61 结局账的列在两份建表脚本里必须一致（替身能模仿真库，不能代替真库）")
    void bothSchemaFilesCarryTheSameWorkflowColumns() throws Exception {
        for (String table : Arrays.asList("z_lc_workflow_binding", "z_lc_workflow_fire")) {
            Set<String> test = columnsOf(table, "schema.sql");
            Set<String> dev = columnsOf(table, "db/schema-h2.sql");
            assertFalse(test.isEmpty(), "z-lc-web 测试建表脚本里找不到 " + table + " 的列");
            assertFalse(dev.isEmpty(), "z-lc-admin dev 建表脚本里找不到 " + table + " 的列");
            assertEquals(dev, test, table + " 在两份脚本里长得不一样（#51/#54/#57 那一族：只在真库才红）");
        }
    }

    @Test
    @DisplayName("#61 发起账的 mapper 必须在 @MapperScan 名单里（漏掉时单测不会红，只有真起服务才红）")
    void fireMapperIsScannedIntoTheLcModule() {
        String[] names = context.getBeanNamesForType(com.zifang.z.lc.mapper.workflow.WorkflowFireMapper.class);
        assertEquals(1, names.length,
                "WorkflowFireMapper 没有起成 bean ⇒ LcModuleDataSource 的 @MapperScan 少了 mapper.workflow，"
                        + "每一次发起的结局都会掉进「insert 不了只剩日志」");
    }

    /* ------------------------------------------------------------------ */
    /* helpers                                                            */
    /* ------------------------------------------------------------------ */

    /** 建一个只有 "case" 实体（一列 ref）的应用并真建表；每次唯一 appCode，避免用例之间串台。 */
    private String provisionedApp(String purpose) throws Exception {
        String app = "itw" + purpose.replace('_', 'x')
                + Long.toString(System.nanoTime() % 100000000L, 36);
        post("/api/lc/admin/app/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\",\"appName\":\"流程绑定探针\"}");
        JsonNode created = post("/api/lc/admin/app/entity/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"entityCode\":\"" + ENTITY + "\",\"entityName\":\"工单\",\"tableName\":\""
                        + "t_wf_" + app + "\",\"fields\":[{\"fieldCode\":\"ref\",\"fieldName\":\"单号\","
                        + "\"fieldType\":\"STRING\",\"fieldLength\":32,\"sortOrder\":1}]}",
                "appCode", app, "tenantCode", TENANT);
        assertOk(created, "建探针实体: " + created.path("message").asText());
        JsonNode prov = post("/api/lc/admin/entity/provision", null, "id",
                created.path("data").path("id").asText());
        assertOk(prov, "建物理表: " + prov.path("message").asText());
        return app;
    }

    private static String bindingJson(String app, String event, String key) {
        return "{\"appCode\":\"" + app + "\",\"entityCode\":\"" + ENTITY
                + "\",\"triggerEvent\":\"" + event + "\",\"processDefinitionKey\":\"" + key + "\"}";
    }

    private JsonNode createBinding(String app, String tail) throws Exception {
        return post(BINDING_CREATE,
                "{\"appCode\":\"" + app + "\",\"entityCode\":\"" + ENTITY + "\","
                        + tail.substring(1));
    }

    /** 写一条记录并拿回主键；{@code expectSuccess=false} 时不做断言（留给"对照"那种形状）。 */
    private long writeRecord(String app, String fieldValuesJson) throws Exception {
        JsonNode res = post("/api/lc/runtime/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"fieldValues\":" + fieldValuesJson + "}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(res, "写记录: " + res.path("message").asText());
        return res.path("data").asLong();
    }

    private long writeRecord(String app, String fieldValuesJson, boolean ignored) throws Exception {
        return writeRecord(app, fieldValuesJson);
    }

    private JsonNode fireRow(String app, long recordId) throws Exception {
        JsonNode rows = get(FIRES, "appCode", app, "entityCode", ENTITY, "recordId", String.valueOf(recordId));
        assertOk(rows, "回读发起账");
        assertEquals(1, rows.path("data").size(),
                "这条记录在账上应该正好一行: " + rows.path("data"));
        return rows.path("data").get(0);
    }

    /** 期望被拒的写入：既钉 HTTP 状态也钉信封 code（z-lc 的坏消息有两种载体，见 {@code LcHttpContractTest}）。 */
    private JsonNode refused(String path, String body) throws Exception {
        JsonNode envelope = post(path, body);
        assertEquals(400, lastHttpStatus, "HTTP 状态应当是 400: " + envelope);
        assertEquals(400, envelope.path("code").asInt(), "信封 code 应当是 400: " + envelope);
        assertFalse(envelope.path("success").asBoolean(), "被拒的不能算成功: " + envelope);
        assertFalse(envelope.path("message").asText().trim().isEmpty(), "拒了总要给个说法: " + envelope);
        return envelope;
    }

    /** 从两份建表脚本里抠出某张表的列名集合（两份的排版风格不同，只有列名这一层可比）。 */
    private static Set<String> columnsOf(String table, String classpathResource) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(classpathResource).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        int from = text.indexOf("CREATE TABLE IF NOT EXISTS " + table);
        if (from < 0) {
            return new LinkedHashSet<String>();
        }
        int open = text.indexOf('(', from);
        int close = text.indexOf("PRIMARY KEY", from);
        if (open < 0 || close < 0) {
            return new LinkedHashSet<String>();
        }
        Set<String> columns = new LinkedHashSet<String>();
        Matcher matcher = Pattern.compile("`([a-z_]+)`").matcher(text.substring(open, close));
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
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
                .content(body == null ? "{}" : (body instanceof String
                        ? (String) body : json.writeValueAsString(body)));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            builder = builder.param(kv[i], kv[i + 1]);
        }
        return call(builder);
    }

    private JsonNode call(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())).andReturn();
        lastHttpStatus = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(body == null || body.isEmpty(), "empty body for " + builder);
        return json.readTree(body);
    }

    private static void assertOk(JsonNode envelope, String context) {
        assertTrue(envelope.path("success").asBoolean(),
                context + " 应当成功，实际 code=" + envelope.path("code").asText()
                        + " message=" + envelope.path("message").asText());
    }
}
