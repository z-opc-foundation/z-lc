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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    private static final String BINDING_UPDATE = "/api/lc/workflow-binding/update";
    private static final String BINDING_LIST = "/api/lc/workflow-binding/list";

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
        assertEquals(1, fireRows(rows).size(), "一条绑定一次发起，账上应该只有一行: " + fireRows(rows));
        assertEquals(1L, rows.path("data").path("total").asLong(),
                "total 得和这一页一起如实（库里就一行）: " + rows.path("data"));
        JsonNode fire = fireRows(rows).get(0);
        assertEquals("STARTED", fire.path("status").asText(), fire.toString());
        assertEquals("wf-stub-77", fire.path("instanceId").asText(),
                "实例 id 必须是 z-wf data.processInstanceId 那一格，不能是整个 data 的 toString: " + fire);
        assertEquals(bindingId, fire.path("bindingId").asText(), fire.toString());
        assertEquals("AFTER_CREATE", fire.path("triggerEvent").asText(), fire.toString());
        assertEquals("expense_approval", fire.path("processDefinitionKey").asText(), fire.toString());
        assertFalse(fire.path("detail").isTextual() && !fire.path("detail").asText().isEmpty(),
                "成功行不该带失败原因: " + fire);

        // 逐条新建都要各发一句：第二条记录不会复用第一条的发起。
        long second = writeRecord(app, "{\"ref\":\"WF-2\"}");
        assertTrue(second > 0);
        assertEquals(2, STUB.count(), "两条记录该发两句: " + STUB.requests());

        // 对照（把"发不发"和"发几条"分开钉）：同一个应用里没有绑定的那个实体，写记录一句都不该多发。
        provisionEntity(app, "plain", "t_wfp_" + app);
        writeRecord(app, "plain", "{\"ref\":\"NB-1\"}");
        assertEquals(2, STUB.count(),
                "没有登记的实体不该发单，否则\"绑定决定发不发\"这句就是假的: " + STUB.requests());
        assertEquals(0, fireRows(get(FIRES, "appCode", app, "entityCode", "plain")).size(),
                "没发单的实体在账上也不该有行");
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
        JsonNode blankKey = refused(BINDING_CREATE, bindingJson(app, "AFTER_CREATE", "   "));
        assertTrue(blankKey.path("message").asText().contains("processDefinitionKey"),
                "空 KEY 要指名是哪一格: " + blankKey);
        assertTrue(refused(BINDING_CREATE, "{\"appCode\":\"" + app + "\",\"entityCode\":\"" + ENTITY
                        + "\",\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p\",\"autoSubmit\":0}")
                .path("message").asText().contains("自动提单"), "关掉自动提单的绑定没有任何运行时行为");

        // 猎物：同样的字段换成引擎真兑现的那一组，必须能登记（否则上面那 5 个 400 是"闸什么都拒"）。
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_ok\"}"),
                "可兑现的那一条应当登记得下来");
        assertEquals(0, STUB.count(), "登记绑定本身不该发起任何流程（只有写记录才发）: " + STUB.requests());

        // 阳性对照：上面那 5 个 400 之所以是"拒了兑现不了的"，得先证明这条链是通的 ——
        // 写一条记录，桩必须收到一句。
        writeRecord(app, "{\"ref\":\"OK-1\"}");
        assertEquals(1, STUB.count(), "登记成功的那一条要真发得出去: " + STUB.requests());
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
        assertEquals("STARTED", fireRow(app, recordId).path("status").asText(),
                "发出去那一句的结局: " + fireRow(app, recordId) + " 桩收到: " + STUB.requests());
    }

    /**
     * 缺陷 #66：查重那一句在 {@code /update} 这条路上**结构上打不到任何行** —— 控制器进门先把
     * {@code tenantCode} 抹成 null（它自己的注释写着"service 负责"），而 service 把补租户排在查重之后。
     * <p>
     * core 层那条 {@code updateShouldRefuseDuplicates} 一直是绿的，因为它候选行的租户是
     * {@code edit()} 从库里抄来的（带着 {@code default}），不是接口真送进来的形状。
     * ⇒ 这一条只能打在真进程边界上：走 {@code /update}，走完还要把清单读回来，
     * 再写一条记录数桩收到几句 —— "两条同 KEY 的绑定都活着"这件事只有从运行结果才看得见。
     */
    @Test
    @DisplayName("#66 把第二条绑定改成与第一条同 KEY：/update 就得在写入口拒，且一个 KEY 只发一句")
    void updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice() throws Exception {
        String app = provisionedApp("dupupdate");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_first\"}"),
                "第一条绑定登记");
        JsonNode second = createBinding(app,
                "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_second\"}");
        assertOk(second, "KEY 不同的第二条登记得下来");
        String secondId = second.path("data").path("id").asText();

        JsonNode collide = refused(BINDING_UPDATE, "{\"id\":" + secondId + ",\"appCode\":\"" + app
                + "\",\"entityCode\":\"" + ENTITY + "\",\"triggerEvent\":\"AFTER_CREATE\","
                + "\"processDefinitionKey\":\"p_first\"}");
        assertTrue(collide.path("message").asText().contains("已经绑定过"),
                "这一句要撞在查重那道上，而不是撞在别的闸上: " + collide);
        assertTrue(collide.path("message").asText().contains("id="),
                "拒绝要说清是被哪一条挡的，否则只能把两条都删了试: " + collide);

        JsonNode list = get(BINDING_LIST, "appCode", app);
        assertOk(list, "回读绑定清单");
        JsonNode stillSecond = null;
        for (JsonNode row : list.path("data")) {
            if (secondId.equals(row.path("id").asText())) {
                stillSecond = row;
            }
        }
        assertNotNull(stillSecond, "被拒的那一条不能从清单里消失: " + list.path("data"));
        assertEquals("p_second", stillSecond.path("processDefinitionKey").asText(),
                "被拒的改动不能已经落到那一行上: " + stillSecond);
        assertEquals(TENANT, stillSecond.path("tenantCode").asText(),
                "拒一次不能顺带把租户搬走: " + stillSecond);

        long recordId = writeRecord(app, "{\"ref\":\"DUPUP-1\"}");
        // 库里活着两条互不重复的绑定（p_first、p_second），一条记录写成功该发两句、一个 KEY 一句。
        // #66 放行后的病灶形状是"两句都是 p_first"：update 把第二条改成了第一条的 KEY，于是同一个流程
        // 对同一条记录被发起两次（两个并行实例）—— 数总句数抓不到它（照样是 2），只有数 KEY 抓得到。
        assertEquals(2, STUB.count(), "两条各不重复的绑定各发一句: " + STUB.requests());
        assertEquals(1, occurrences(STUB.allBodies(), "\"processKey\":\"p_first\""),
                "同一个 KEY 对一条记录只能发一句，发两句说明库里留了两行同 KEY 的绑定: " + STUB.allBodies());
        assertEquals(1, occurrences(STUB.allBodies(), "\"processKey\":\"p_second\""),
                "被拒的那一条不该顺带丢掉自己原来的 KEY: " + STUB.allBodies());
        JsonNode rows = get(FIRES, "appCode", app, "entityCode", ENTITY, "recordId", String.valueOf(recordId));
        assertOk(rows, "回读发起账");
        assertEquals(2, fireRows(rows).size(),
                "两条各不重复的绑定该留下两行发起账: " + fireRows(rows));
    }

    /**
     * `/fires` 这一页的行。
     * <p>
     * 形状读不出来就<b>抛</b>，不许退化成"0 行"：这一族里有好几条判的是"账上正好 0 行"
     * （没发单的实体、别人的租户），信封一漂移它们会集体假绿 —— 而 0 恰恰是它们想要的值。
     * {@code total} 缺席同理：缺陷 #65 的整个修法就是"总数要如实回出来"。
     */
    private static JsonNode fireRows(JsonNode envelope) {
        JsonNode data = envelope.path("data");
        JsonNode records = data.path("records");
        if (!records.isArray()) {
            throw new AssertionError("/fires 的回包不是 {data:{records:[...]}} 形状: " + data);
        }
        if (!data.path("total").isNumber()) {
            throw new AssertionError("/fires 没有回 total（那一格缺席就等于截断又变得不可见）: " + data);
        }
        return records;
    }

    /** {@code needle} 在 {@code haystack} 里出现几次（重叠不算）。 */
    private static int occurrences(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
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
    @DisplayName("#61 引擎回 5xx 而 body 写着成功：那是没起来，账上不能记成 STARTED")
    void http5xxWithASuccessfulLookingBodyIsNotAFire() throws Exception {
        // 这一格形状是量具逼出来的，不是设想出来的：注入 M5（把 `if (!CtcAdapter.httpAccepted(res))`
        // 摘回 `if (!res.isSuccess())`）在 core 层红两条、在这一层**一条都不红**。查下来是这一族
        // 此前只有"200 + success=false"一种拒绝形状，而 z-util-http 对任何**完成**的响应都置
        // success=true（字节码 iconst_1 → putfield success）⇒ "判成功看状态码还是看信封"这个决定
        // 在运行时无处被检验。补上 5xx+成功 body 这一格，M5 才有第四层（真进程边界）的牙。
        String app = provisionedApp("fivexx");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_5xx\"}"),
                "绑定登记");
        // body 仍是那句带 processInstanceId 的成功信封，只有状态码换成 502 ——
        // 这正是"网关把后端的 200 应答连着 502 一起吐出来"的形态：那一句流程很可能根本没起。
        STUB.status(502);

        long recordId = writeRecord(app, "{\"ref\":\"HX-1\"}");
        assertTrue(recordId > 0, "引擎回 5xx，用户这条记录照样要落地");
        JsonNode fire = fireRow(app, recordId);
        assertEquals("FAILED", fire.path("status").asText(),
                "5xx 时 body 里那个 processInstanceId 一个字都不能信: " + fire);
        assertFalse(fire.path("instanceId").asText().contains("wf-stub-77"),
                "把一次没发生的发起记成有实例号，等于留下一条查无此单的账: " + fire);
        assertTrue(fire.path("detail").asText().contains("http=502"),
                "失败原因要说清是 http 几，只写「发起失败」等于让人去翻日志: " + fire);
        assertEquals(1, STUB.count(), "这一支测的是「打出去了、引擎用 5xx 说话」: " + STUB.requests());

        // 阳性对照：状态码换回 200、body 一字不动 ⇒ 必须算一次成功发起。少了这一句，上面的
        // FAILED 就分不开"5xx 判得对"和"这一格恒 FAILED"（#48 收口时"0 要有阳性对照"同一条）。
        STUB.reset();
        long ok = writeRecord(app, "{\"ref\":\"HX-2\"}");
        assertEquals("STARTED", fireRow(app, ok).path("status").asText(),
                "同一个 body 在 200 下必须算成功，否则上面那三条是常数: " + STUB.requests());
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
        assertEquals(1, STUB.count(), "这一支测的是「打出去了但回得慢」，桩一句都没收到就什么都没测: "
                + STUB.requests());
        // 上面两条只证明"调用方到点走了"。走掉之后那条 HTTP 调用还挂在共享客户端的 60s 读超时上，
        // 派发池的槽位也跟着被按住 —— 这一条才是这一支的牙齿：连接必须在有上限之后还回来。
        assertTrue(STUB.awaitIdle(2000L),
                "挂死的那一次发起没有在有上限之后把槽位还回来（桩这边还压着 " + STUB.openCount()
                        + " 条连接）");
    }

    @Test
    @DisplayName("#61 一次超时会传染下一次发起吗：挂死要在有上限之后松手，随后连发三条各自成功")
    void timeoutDoesNotPoisonTheNextFires() throws Exception {
        // 动机（这一条是实测逼出来的，不是设想）：契约层第一次跑通"回得慢"之后，紧跟它的
        // 三条无因地读不到发起 —— 日志里是 WorkflowTriggerDispatcher 那句「并发发起已达上限
        // （2 个在飞、不排队）」。查下来是"等待有上限"只做在了调用方：future.get(250ms) 到点
        // 返回了，而那条在途 HTTP 调用借的是共享 z-util-http 客户端（读超时 60s），
        // cancel(true) 只中断线程、中断不了 okhttp 的阻塞读 ⇒ 槽位被按住到 60s，两次挂死就把
        // 整个 2 槽池抽干，之后每一条记录的绑定都"什么都没发"。
        // 所以这一条要的是"一次超时不许传染下一次"，靠 delay(3000) 让挂死时长跨过修复后的上限：
        // 修好后连接在 ~750ms 归还 ⇒ 后面三条全绿；没修就是桩自己睡到 3000ms 才松手 ⇒ 这里红。
        String app = provisionedApp("poison");
        assertOk(createBinding(app, "{\"triggerEvent\":\"AFTER_CREATE\",\"processDefinitionKey\":\"p_poison\"}"),
                "绑定登记");

        STUB.delay(3000);
        long slow = writeRecord(app, "{\"ref\":\"P-0\"}");
        assertEquals("FAILED", fireRow(app, slow).path("status").asText(),
                "前置条件没成立：这一次超时没有发生，那这一条什么都没测");
        assertEquals(1, STUB.count(), "前置条件：那一句挂死的发起要真的打到桩上: " + STUB.requests());
        assertTrue(STUB.awaitIdle(1500L),
                "挂死那一次没有在有上限之后松手（派发槽位还被按着，桩这边压着 " + STUB.openCount()
                        + " 条连接）");
        STUB.reset();

        for (int i = 1; i <= 3; i++) {
            long id = writeRecord(app, "{\"ref\":\"P-" + i + "\"}");
            JsonNode row = fireRow(app, id);
            assertEquals("STARTED", row.path("status").asText(),
                    "第 " + i + " 条被上一条超时带坏了（脏连接复用）: " + row);
        }
        // reset 会清掉已记的请求，所以这里数的是超时之后那三条。
        assertEquals(3, STUB.count(), "超时之后应当连发三句: " + STUB.requests());
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
        assertEquals(0, fireRows(get(FIRES, "appCode", app, "recordId", String.valueOf(foreign)))
                .size(), "别人的发起也不该出现在本租户的账里");

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
        assertTrue(undone.path("data").path("applied").asBoolean(),
                "撤销没有真的动任何一行，那后面的「重做没发单」是空跑: " + undone);
        JsonNode redone = post("/api/lc/undo/redo",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app + "\"}",
                "entityCode", ENTITY, "appCode", app, "tenantCode", TENANT);
        assertOk(redone, "重做刚才那条: " + redone.path("message").asText());
        // 上面两个 applied 断言是这一条的牙齿：只钉"桩没收到"的话，"什么都没撤销所以没重做"
        // 也会绿（#48 收口时那条"0 要有阳性对照"的同一个形状）。
        assertTrue(redone.path("data").path("applied").asBoolean(),
                "重做没有回放任何一行: " + redone);
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
            Set<String> test = columnsOf(table, new File(repoRoot(), "z-lc-web/src/test/resources/schema.sql"));
            Set<String> dev = columnsOf(table,
                    new File(repoRoot(), "z-lc-admin/src/main/resources/db/schema-h2.sql"));
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

    /**
     * 缺陷 #65 的契约层：结局账的<b>窗口</b>要如实。
     * <p>
     * 为什么这一条不能只留在 core 单测里：{@code page}/{@code size} 是 HTTP 查询参数，
     * 控制器签名漏掉任何一个（或 {@code @RequestParam} 名字打错）在 core 层都是全绿的 ——
     * "界面翻页没反应"正是这种形状。同理 {@code total} 必须由接口真的回出来，
     * 前一句「共 250 条」才有出处；静默截断之所以能活这么久，就是因为读侧没有任何一格
     * 说得出"库里一共有多少"。
     */
    @Test
    @DisplayName("#65 结局账的窗口如实：total 是真总数、page/size 真的参与查询、上限回显的是收口后的值")
    void fireLedgerReportsItsWholeWindow() throws Exception {
        String app = provisionedApp("window");
        for (long i = 1; i <= 25; i++) {
            seedFireRow(app, 9000L + i);
        }

        JsonNode first = get(FIRES, "appCode", app, "entityCode", ENTITY, "page", "1", "size", "10");
        assertOk(first, "读第一页");
        assertEquals(25L, first.path("data").path("total").asLong(),
                "total 必须是库里的真行数，不是这一页读到的条数: " + first.path("data"));
        assertEquals(10, fireRows(first).size(), "一页就是问的 10 条: " + fireRows(first));
        assertEquals(1L, first.path("data").path("pageNum").asLong(), "回显问的那一页");
        assertEquals(10L, first.path("data").path("pageSize").asLong(), "回显收口后的页大小");

        JsonNode second = get(FIRES, "appCode", app, "entityCode", ENTITY, "page", "2", "size", "10");
        assertNotEquals(idsOf(fireRows(first)).toString(), idsOf(fireRows(second)).toString(),
                "翻到第 2 页还是第 1 页的内容 ⇒ offset 根本没参与查询（LIMIT 只写了 count）");
        assertEquals(25L, second.path("data").path("total").asLong(),
                "每翻一页 total 都不许变，它不是这一页的计数");

        JsonNode third = get(FIRES, "appCode", app, "entityCode", ENTITY, "page", "3", "size", "10");
        assertEquals(5, fireRows(third).size(),
                "25 条的第 3 页只剩 5 条（LIMIT 20,10）: " + fireRows(third));

        JsonNode past = get(FIRES, "appCode", app, "entityCode", ENTITY, "page", "9", "size", "10");
        assertTrue(fireRows(past).isEmpty(), "问过头的那一页该是空的: " + fireRows(past));
        assertEquals(25L, past.path("data").path("total").asLong(),
                "空页也必须如实报总数 —— 界面那句「还没有发起记录」只能由 total=0 来说，"
                        + "由空页来说就是缺陷 #65 换了个地方复发");

        JsonNode clamped = get(FIRES, "appCode", app, "entityCode", ENTITY, "page", "1", "size", "5000");
        assertEquals(200L, clamped.path("data").path("pageSize").asLong(),
                "页大小要按上限收口（一次读不许把整张表拉进内存），且回显的是**收口后**的值: "
                        + clamped.path("data"));
        assertEquals(25, fireRows(clamped).size(), "收口不影响这里读到的行数（库里只有 25 条）");
        assertEquals(25L, clamped.path("data").path("total").asLong(), "收口后 total 仍然是真总数");

        // 阳性对照：这一格读不到别人的账，也读不到不存在的实体（否则上面那一串 25 是"整表扫"给的）
        assertEquals(0L, get(FIRES, "appCode", app, "entityCode", "no-such-entity")
                .path("data").path("total").asLong(),
                "total 不能是「整个 app 的行数」—— 谓词得和取数那一条一模一样");
        assertEquals(0, fireRows(get(FIRES, "appCode", app + "-other", "entityCode", ENTITY)).size(),
                "别的应用的结局不能进这个窗口（上面那个 0 的阳性对照）");
    }

    private static List<Long> idsOf(JsonNode rows) {
        List<Long> out = new ArrayList<Long>();
        for (JsonNode row : rows) {
            out.add(row.path("recordId").asLong());
        }
        return out;
    }

    /** 直接种结局行：这一族要的是"账上有 25 条"这个事实，不是走一遍真派发。 */
    private void seedFireRow(String app, long recordId) {
        jdbc().update("INSERT INTO z_lc_workflow_fire (tenant_code, app_code, entity_code, record_id,"
                        + " binding_id, trigger_event, process_definition_key, status, instance_id,"
                        + " create_time, update_time, deleted) VALUES (?,?,?,?,?,?,?,?,?,?,?,0)",
                TENANT, app, ENTITY, recordId, 1L, "AFTER_CREATE", "p_window", "STARTED",
                "wf-" + recordId, new java.util.Date(), new java.util.Date());
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
        provisionEntity(app, ENTITY, "t_wf_" + app);
        return app;
    }

    /** 真建一个只有一列 {@code ref} 的实体，并 provision 出物理表（#47 之后运行时读的是真表）。 */
    private void provisionEntity(String app, String entityCode, String table) throws Exception {
        JsonNode created = post("/api/lc/admin/app/entity/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"entityCode\":\"" + entityCode + "\",\"entityName\":\"实体 " + entityCode
                        + "\",\"tableName\":\"" + table
                        + "\",\"fields\":[{\"fieldCode\":\"ref\",\"fieldName\":\"单号\","
                        + "\"fieldType\":\"STRING\",\"fieldLength\":32,\"sortOrder\":1}]}",
                "appCode", app, "tenantCode", TENANT);
        assertOk(created, "建探针实体 " + entityCode + ": " + created.path("message").asText());
        JsonNode prov = post("/api/lc/admin/entity/provision", null, "id",
                created.path("data").path("id").asText());
        assertOk(prov, "建物理表: " + prov.path("message").asText());
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

    /** 写一条 {@code case} 记录并拿回主键。 */
    private long writeRecord(String app, String fieldValuesJson) throws Exception {
        return writeRecord(app, ENTITY, fieldValuesJson);
    }

    private long writeRecord(String app, String entityCode, String fieldValuesJson) throws Exception {
        JsonNode res = post("/api/lc/runtime/create",
                "{\"tenantCode\":\"" + TENANT + "\",\"appCode\":\"" + app
                        + "\",\"fieldValues\":" + fieldValuesJson + "}",
                "entityCode", entityCode, "appCode", app, "tenantCode", TENANT);
        assertOk(res, "写记录: " + res.path("message").asText());
        return res.path("data").asLong();
    }

    private JsonNode fireRow(String app, long recordId) throws Exception {
        JsonNode rows = get(FIRES, "appCode", app, "entityCode", ENTITY, "recordId", String.valueOf(recordId));
        assertOk(rows, "回读发起账");
        assertEquals(1, fireRows(rows).size(),
                "这条记录在账上应该正好一行: " + fireRows(rows));
        return fireRows(rows).get(0);
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
    private static Set<String> columnsOf(String table, File script) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(script), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        int from = text.indexOf("CREATE TABLE IF NOT EXISTS " + table);
        if (from < 0) {
            return new LinkedHashSet<String>();
        }
        int open = text.indexOf("(", from);
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

    /**
     * 两份脚本不在同一个模块里，而 z-lc-web 不依赖 z-lc-admin（所以第二份不可能在 classpath 上），
     * 只能按仓库结构去够。surefire 的工作目录是模块目录，故向上找到"看得见 z-lc-admin/pom.xml"
     * 的那一层。找不到就当场报错 —— 静默跳过会让这一条变成"永远绿"的空跑。
     */
    private static File repoRoot() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int depth = 0; depth < 5 && dir != null; depth++) {
            if (new File(dir, "z-lc-admin/pom.xml").isFile() && new File(dir, "z-lc-web/pom.xml").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("从 " + System.getProperty("user.dir")
                + " 向上找不到 z-lc 仓库根（两份建表脚本没法比，这一条断言无效）");
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
