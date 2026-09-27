package com.zifang.z.lc.web.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.core.deployment.DeploymentTypes;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 #70 的契约层：一条部署记录到底换出了什么 —— 真起上下文、真打 HTTP、真看物理表落没落地.
 * <p>
 * 修之前的形状（07:5x–08:0x 在跑的 dev jar 上实测，台账见 {@code _e2e/README.md}「缺陷 #70」）：
 * {@code DeploymentController.create} 是 insert 一行 PENDING + {@code // TODO: 异步执行物化/部署逻辑}
 * 就返回，而全仓没有任何执行器（{@code updateDeploymentStatus} 生产代码零调用者），于是
 * <ol>
 *   <li>PENDING 就是终态：id=2 那行 07:54:44 建、07:57:18 回读仍 {@code PENDING}，且
 *     {@code update_time} 与 {@code create_time} 逐字相同 —— 没有一个写家碰过它；</li>
 *   <li>不存在的 appCode 也收（id=5 {@code no_such_app_zzz}：200 + 一行账）；</li>
 *   <li>{@code materializationId} 随手填 999999 也收（id=3，指向一个不存在的批次）；</li>
 *   <li>{@code deploy_type} 是 NOT NULL，不给值就是一句裸 500
 *     {@code NULL not allowed for column "deploy_type"}（数据库原话外泄，同 #34 那一族）；</li>
 *   <li>界面上三个「部署方式」（热加载 / Docker 镜像 / Git 推送）选哪个都不执行。</li>
 * </ol>
 * 所以这一层每条断言都不看"接口回了 200"，只看<b>库里与运行时真多了什么</b>：表在不在、
 * 记录写不写得进、那一行的 {@code status} 与 {@code deploy_log} 说的是什么。
 */
@SpringBootTest(classes = LcTestApplication.class)
@AutoConfigureMockMvc
class DeploymentContractTest {

    private static final String TENANT = "default";
    private static final String ENTITY = "order";
    private static final String CREATE = "/api/lc/deployment/create";
    private static final String LIST = "/api/lc/deployment/list";
    private static final String DETAIL = "/api/lc/deployment/detail";
    private static final String VOCABULARY = "/api/lc/deployment/vocabulary";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private javax.sql.DataSource dataSource;

    private int lastHttpStatus;

    /* ------------------------------------------------------------------ */
    /* 1. 正向：部署真的把定义落到运行时库，并把结局写回那一行                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 部署真的建表：定义先不落库，一次部署之后表在、记录写得进、账上是 SUCCESS")
    void deploymentActuallyLandsTheDefinitionAndRecordsTheOutcome() throws Exception {
        String table = probeTable("land");
        String app = appWithUnprovisionedEntity("land", table);
        assertFalse(tableExists(table), "前置条件：实体建好时物理表不该已经在运行时库里 —— 表本来就在的话，"
                + "后面那句 SUCCESS 就无从归属给这次部署");

        JsonNode created = post(CREATE, deployBody(app, DeploymentTypes.HOT_LOAD));
        assertOk(created, "登记一次 HOT_LOAD 部署: " + created.path("message").asText());
        // 这一条就是缺陷本体：回答的必须是执行后的真状态，不是一句"我先收下，回头再说"。
        assertEquals("SUCCESS", created.path("data").path("status").asText(),
                "HOT_LOAD 部署完应当当场给出 SUCCESS/FAILED，而不是留一个没人会改的 PENDING: " + created);
        String log = created.path("data").path("deployLog").asText();
        assertTrue(log.contains("1 个实体") && log.contains("新建 1 张"),
                "deploy_log 要说清这一次真的动了什么: " + log);

        // 响应里那句 SUCCESS 必须在库里对上，否则它只是响应体的修辞。
        assertTrue(tableExists(table), "部署说 SUCCESS 而物理表不在 —— 界面状态列就是骗人的: " + log);
        assertTrue(writeRecord(app, "{\"ref\":\"DEP-1\"}") > 0,
                "部署出来的表必须真的能用（写一条进得去）");

        String id = created.path("data").path("id").asText();
        JsonNode detail = get(DETAIL, "id", id);
        assertOk(detail, "回读部署详情");
        assertEquals("SUCCESS", detail.path("data").path("status").asText(),
                "详情读回来的状态要跟写进去的一致（否则「状态列」和「日志抽屉」各说一套）: " + detail);
        assertFalse(detail.path("data").path("deployLog").asText().trim().isEmpty(),
                "界面上那个「日志」抽屉此前恒为「（暂无日志）」: " + detail);

        JsonNode rows = get(LIST, "appCode", app).path("data");
        assertEquals(1, rows.size(), "这次部署要在本租户的清单里读得回: " + rows);
        assertEquals("SUCCESS", rows.get(0).path("status").asText(), rows.toString());
    }

    /* ------------------------------------------------------------------ */
    /* 2. 拒绝面：兑现不了的方式不出生，且"什么都没发生"要证得出               */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 服务器不会执行的方式在写入口就被拒，且库里一行都不留、表也不在")
    void typesWithoutAnExecutorAreRefusedAndLeaveNothingBehind() throws Exception {
        String table = probeTable("refuse");
        String app = appWithUnprovisionedEntity("refuse", table);
        Map<String, String> reasons = DeploymentTypes.unimplementedReasons();

        for (String rejected : reasons.keySet()) {
            JsonNode out = refused(CREATE, deployBody(app, rejected));
            assertTrue(out.path("message").asText().contains(rejected),
                    "拒绝要点名是哪一种方式: " + out);
            assertTrue(out.path("message").asText().contains(DeploymentTypes.HOT_LOAD),
                    "同时要说出当前可用的是哪一种: " + out);
            assertTrue(out.path("message").asText().contains(reasons.get(rejected)),
                    "拒绝理由必须与词表里那一句同源（两处各写一份就会漂）: " + out);
        }
        JsonNode list = get(LIST, "appCode", app);
        assertEquals(0, list.path("data").size(),
                "被拒的部署不该留下一行永远 PENDING 的账: " + list.path("data"));
        assertFalse(tableExists(table), "被拒的不该顺手把表建出来（拒绝的语义是「什么都没做」）");

        // 阳性对照：同一个应用换个真有执行器的方式 ⇒ 必须立刻成功。
        // 少了这一句，上面那个"一行都不留"分不开"闸管用"和"这个端点根本写不进账"（#48 同一条）。
        assertOk(post(CREATE, deployBody(app, DeploymentTypes.HOT_LOAD)), "对照：HOT_LOAD 该收下");
        assertEquals(1, get(LIST, "appCode", app).path("data").size(), "对照之后清单里该正好一行");
        assertTrue(tableExists(table), "对照：表该由这次 HOT_LOAD 建出来");
    }

    /* ------------------------------------------------------------------ */
    /* 3. 写入口不再什么都收：空白、幻影应用、幻影批次、别人的批次              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 缺 deployType 是 400 而不是把数据库的 NOT NULL 原话外泄成 500")
    void missingDeployTypeIsRefusedWithoutLeakingTheDatabaseError() throws Exception {
        String app = appWithUnprovisionedEntity("notype", probeTable("notype"));

        JsonNode missing = refused(CREATE, "{\"appCode\":\"" + app + "\"}");
        assertTrue(missing.path("message").asText().contains("deployType"), missing.toString());
        assertFalse(missing.path("message").asText().toLowerCase().contains("null not allowed"),
                "数据库那一层的原话不该出现在接口上（实测修前是 500 带 "
                        + "NULL not allowed for column \"deploy_type\"）: " + missing);
        assertEquals(400, lastHttpStatus, "裸 500 换成 400 才是这一条的牙齿");

        JsonNode noApp = refused(CREATE, "{\"deployType\":\"" + DeploymentTypes.HOT_LOAD + "\"}");
        assertTrue(noApp.path("message").asText().contains("appCode"), noApp.toString());

        String ghost = "no_such_app_" + System.nanoTime() % 100000000L;
        JsonNode ghostRefused = refused(CREATE, deployBody(ghost, DeploymentTypes.HOT_LOAD));
        assertTrue(ghostRefused.path("message").asText().contains("没有应用"),
                "部署一个不存在的应用：修前是 200 + 一行账（实测 id=5）: " + ghostRefused);
        assertEquals(0, get(LIST, "appCode", ghost).path("data").size(), "幻影应用的清单必须还是空的");
    }

    @Test
    @DisplayName("#70 物化批次那一格要么指得回真批次，要么别说：幻影与串应用都被拒")
    void materializationBatchMustExistAndBelongToThisApp() throws Exception {
        String app = appWithUnprovisionedEntity("batch", probeTable("batch"));
        long foreignBatch = insertBatch("other-app-" + System.nanoTime() % 100000000L, "other-tenant");
        long ownBatch = insertBatch(app, TENANT);

        JsonNode ghost = refused(CREATE, "{\"appCode\":\"" + app + "\",\"deployType\":\""
                + DeploymentTypes.HOT_LOAD + "\",\"materializationId\":999999}");
        assertTrue(ghost.path("message").asText().contains("999999"),
                "修前实测：随手一个 999999 直接进账并原样回显（id=3）: " + ghost);

        JsonNode foreign = refused(CREATE, batchBody(app, foreignBatch));
        assertTrue(foreign.path("message").asText().contains("不能挂在"),
                "别的应用/别的租户的批次不能挂到这条部署上（与 #48 的租户口径同源）: " + foreign);
        assertEquals(0, get(LIST, "appCode", app).path("data").size(), "两支被拒都不该留账");

        // 阳性对照：本应用本租户的真批次 ⇒ 收下且原样读得回，否则上面那两条是常数。
        JsonNode ok = post(CREATE, batchBody(app, ownBatch));
        assertOk(ok, "挂本应用自己的真批次该被接受: " + ok.path("message").asText());
        assertEquals(ownBatch, ok.path("data").path("materializationId").asLong(), ok.toString());
        assertEquals(ownBatch, get(DETAIL, "id", ok.path("data").path("id").asText())
                .path("data").path("materializationId").asLong(),
                "界面上那一列叫「物化批次」，它得真的指得回一批存在的产物");
    }

    /* ------------------------------------------------------------------ */
    /* 4. 词表与门口行为必须是一套说法（界面清单的唯一来源）                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 /vocabulary 说的「会执行/不会执行」与写入口实际行为逐条一致")
    void vocabularyMatchesWhatTheDoorActuallyDoes() throws Exception {
        JsonNode vocab = get(VOCABULARY);
        assertOk(vocab, "读部署方式词表");
        JsonNode executable = vocab.path("data").path("executable");
        assertTrue(executable.isArray() && executable.size() > 0,
                "词表得说出至少一种真会执行的方式，否则界面没有任何可选项: " + vocab);
        List<String> names = new ArrayList<String>();
        for (JsonNode node : executable) {
            names.add(node.asText());
        }
        assertEquals(DeploymentTypes.implemented(), names,
                "词表必须就是引擎那一份，不能在 HTTP 层再手抄一遍（#61 的前端手抄就是这么漂的）");

        JsonNode rejected = vocab.path("data").path("rejected");
        assertTrue(rejected.size() >= 2,
                "界面上出现过又被摘掉的方式要在词表里说清为什么（DOCKER/GIT_PUSH 至少这两条）: " + rejected);
        for (JsonNode node : rejected) {
            assertFalse(node.path("reason").asText().trim().isEmpty(),
                    "只列名字不列原因，界面就没法把「为什么不能选」讲给用户: " + node);
        }

        // 词表与门口逐条一致：executable 里每一种都要真收得下，rejected 里每一种都要真被拒。
        String app = appWithUnprovisionedEntity("vocab", probeTable("vocab"));
        for (String name : names) {
            assertOk(post(CREATE, deployBody(app, name)), "词表说 [" + name + "] 会执行，门口就得收");
        }
        for (JsonNode node : rejected) {
            String name = node.path("type").asText();
            assertTrue(refused(CREATE, deployBody(app, name)).path("message").asText().contains(name),
                    "词表说 [" + name + "] 不执行，门口就得拒");
        }
        assertEquals(names.size(), get(LIST, "appCode", app).path("data").size(),
                "被拒的一条都不许进账 ⇒ 清单行数必须正好等于可执行方式的条数: "
                        + get(LIST, "appCode", app).path("data"));
    }

    /* ------------------------------------------------------------------ */
    /* 5. 落不成就该红着记账，而不是"部署已创建"                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 定义落不成时账上是 FAILED 且点名哪一支，不是一句「部署已创建」")
    void aDefinitionThatCannotLandIsRecordedAsFailedAndNamesTheEntity() throws Exception {
        String table = probeTable("notours");
        // 一张不归本引擎的表先占着这个名字（没有那五个自建列）：这正是类注释里说的
        // "别的系统占了同一张表"，也是 provision 判 FAILED 的那一支。建定义那一侧拦不住它，
        // 因为抢名是在"另一个未删除实体"这个口径上判的。
        jdbc().execute("CREATE TABLE " + table + " (ref VARCHAR(32))");
        String app = appWithUnprovisionedEntity("notours", table, /*tableAlreadyThere*/ true);

        JsonNode created = post(CREATE, deployBody(app, DeploymentTypes.HOT_LOAD));
        assertOk(created, "部署请求本身是合法的（失败在执行结果，不在入参）");
        assertEquals("FAILED", created.path("data").path("status").asText(),
                "有一支没建成就不能报 SUCCESS: " + created);
        String log = created.path("data").path("deployLog").asText();
        assertTrue(log.contains("没建成 1 张"), "计数要如实: " + log);
        assertTrue(log.contains(ENTITY), "要点名是哪一支没建成: " + log);
        assertTrue(log.contains("[" + ProvisionReport.FAILED + "]"), "逐支要带各自的状态: " + log);
        assertEquals("FAILED", get(DETAIL, "id", created.path("data").path("id").asText())
                .path("data").path("status").asText(), "失败也要在库里读得回，不能只在响应里红一下");

        // 阳性对照：干净应用同一套判据 ⇒ 必须给 SUCCESS。否则上面四条是恒 FAILED 的假尺。
        String cleanTable = probeTable("clean");
        String clean = appWithUnprovisionedEntity("clean", cleanTable);
        assertEquals("SUCCESS", post(CREATE, deployBody(clean, DeploymentTypes.HOT_LOAD))
                        .path("data").path("status").asText(),
                "干净应用也判 FAILED 的话，这一条就没有猎物");
        assertTrue(tableExists(cleanTable), "对照：干净那份该真把表建出来了");
    }

    /* ------------------------------------------------------------------ */
    /* 6. 租户：body 说的租户不算数，否则写进去就读不回来                     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("#70 body 里塞别的租户不能把这条部署搬出本租户清单（/list 只按 default 过滤）")
    void bodyTenantCannotMoveTheRowOutOfItsOwnList() throws Exception {
        String table = probeTable("tenant");
        String app = appWithUnprovisionedEntity("tenant", table);
        JsonNode created = post(CREATE, "{\"tenantCode\":\"someone-else\",\"appCode\":\"" + app
                + "\",\"deployType\":\"" + DeploymentTypes.HOT_LOAD + "\"}");
        assertOk(created, "登记部署: " + created.path("message").asText());
        assertEquals(TENANT, created.path("data").path("tenantCode").asText(),
                "/list 只按 default 过滤，写进 someone-else 就等于这行账谁也看不见（#48 的 ③ 同形）");
        assertEquals(1, get(LIST, "appCode", app).path("data").size(), "写进去必须读得回来");
        assertTrue(tableExists(table),
                "部署是对 default 这个租户下的这个应用执行的，表要真的落下来");
    }

    /* ------------------------------------------------------------------ */
    /* helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static String deployBody(String app, String deployType) {
        return "{\"appCode\":\"" + app + "\",\"deployType\":\"" + deployType + "\"}";
    }

    private static String batchBody(String app, long batchId) {
        return "{\"appCode\":\"" + app + "\",\"deployType\":\"" + DeploymentTypes.HOT_LOAD
                + "\",\"materializationId\":" + batchId + "}";
    }

    /** 探针表名：整条用例里唯一，且不与别的用例的表抢。 */
    private static String probeTable(String purpose) {
        return "t_dep_" + purpose.replaceAll("[^a-z0-9]", "") + System.nanoTime() % 100000000L;
    }

    /** 建一个只有一列 {@code ref} 的实体，<b>不</b> provision —— 物理表此时不该由本引擎建出来。 */
    private String appWithUnprovisionedEntity(String purpose, String table) throws Exception {
        return appWithUnprovisionedEntity(purpose, table, false);
    }

    private String appWithUnprovisionedEntity(String purpose, String table, boolean tablePreexists)
            throws Exception {
        String app = "itd" + purpose.replaceAll("[^a-z0-9]", "")
                + Long.toString(System.nanoTime() % 100000000L, 36);
        JsonNode created = post("/api/lc/admin/app/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\",\"appName\":\"部署探针\"}");
        assertOk(created, "建应用 " + app + ": " + created.path("message").asText());
        JsonNode entity = post("/api/lc/admin/app/entity/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"entityCode\":\"" + ENTITY + "\",\"entityName\":\"实体 " + ENTITY
                        + "\",\"tableName\":\"" + table
                        + "\",\"fields\":[{\"fieldCode\":\"ref\",\"fieldName\":\"单号\","
                        + "\"fieldType\":\"STRING\",\"fieldLength\":32,\"sortOrder\":1}]}",
                "appCode", app, "tenantCode", TENANT);
        assertOk(entity, "建实体 " + ENTITY + "（表" + (tablePreexists ? "已被占" : "未落") + "）: "
                + entity.path("message").asText());
        return app;
    }

    private boolean tableExists(String table) {
        Integer n = jdbc().queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?",
                Integer.class, table.toUpperCase());
        return n != null && n.intValue() > 0;
    }

    /** 直接种一行物化批次（走 trigger 会真往 {@code ~/z-lc-materialized} 写文件，这里只要那行账）。 */
    private long insertBatch(String appCode, String tenantCode) {
        jdbc().update("INSERT INTO z_lc_materialization (tenant_code, app_code, materialization_path, "
                        + "export_version, status, file_count, description, trigger_source, deleted, "
                        + "create_time, update_time) VALUES (?, ?, 'it-dep', 'v-it', 'READY', 0, "
                        + "'契约层探针', 'USER', 0, NOW(), NOW())", tenantCode, appCode);
        Long id = jdbc().queryForObject("SELECT MAX(id) FROM z_lc_materialization", Long.class);
        assertTrue(id != null && id.longValue() > 0, "种不进物化批次 ⇒ 这一条没有猎物");
        return id.longValue();
    }

    private long writeRecord(String app, String fieldValuesJson) throws Exception {
        JsonNode res = post("/api/lc/runtime/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"fieldValues\":" + fieldValuesJson + "}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(res, "写记录: " + res.path("message").asText());
        return res.path("data").asLong();
    }

    private org.springframework.jdbc.core.JdbcTemplate jdbc() {
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }

    /** 期望被拒的写入：HTTP 状态与信封 code 一起钉（z-lc 的坏消息有两种载体，见 LcHttpContractTest）。 */
    private JsonNode refused(String path, String body) throws Exception {
        JsonNode envelope = post(path, body);
        assertEquals(400, lastHttpStatus, "HTTP 状态应当是 400: " + envelope);
        assertEquals(400, envelope.path("code").asInt(), "信封 code 应当是 400: " + envelope);
        assertFalse(envelope.path("success").asBoolean(), "被拒的不能算成功: " + envelope);
        assertFalse(envelope.path("message").asText().trim().isEmpty(), "拒了总要给个说法: " + envelope);
        return envelope;
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
