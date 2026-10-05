package com.zifang.z.lc.core.adapter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * CamudaAdapter 单元测试 —— 这一层钉的是「打出去的那一句 z-camuda 真的受理，收回来的那个 id 真的能用」.
 * <p>
 * 缺陷 #61 之前这个方法在生产代码里零调用者，所以它的三处契约错了一次都没被撞过：
 * 路径少了 {@code /api} 前缀且 {@code process} 少一个 s、body 发的键是 {@code processDefKey}
 * 而 DTO 读 {@code processKey}、成功时把整个 {@code data} Map 的 toString 当实例 id。
 * 每条断言都配了猎物：正向那份请求真的落到 {@link CamudaStubServer} 上并被逐字读回，
 * 反向（旧写法）在同一个断言里就过不去。
 */
public class CamudaAdapterTest {

    private CamudaAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new CamudaAdapter();
        inject(adapter, "baseUrl", "http://localhost:8888");
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("CamudaAdapter 应当标注 @Component", CamudaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("CamudaAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(CamudaAdapter.class));
    }

    @Test
    public void nameShouldBeWf() {
        assertEquals("wf", adapter.name());
        assertEquals("wf", CamudaAdapter.NAME);
    }

    @Test
    public void priorityShouldBeTwentyFive() {
        assertEquals(25, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        adapter.init();
    }

    /**
     * 路径必须等于 z-camuda 真实映射的那一条：{@code z-camuda-web/.../ApprovalCenterController.java} 的
     * {@code @RequestMapping("/api/approval-center")} + {@code @PostMapping("/processes/start")}。
     * 旧写法 {@code /approval-center/process/start} 在这条断言下当场红（两个错各红一次）。
     */
    @Test
    public void postsToThePathZwfActuallyMaps() throws Exception {
        inject(adapter, "baseUrl", "http://127.0.0.1:1");
        assertEquals("http://127.0.0.1:1/api/approval-center/processes/start", adapter.startUrl());

        try (CamudaStubServer stub = CamudaStubServer.ok()) {
            inject(adapter, "baseUrl", stub.baseUrl());
            adapter.startProcess("expense", "biz-1", null, null, null);
            assertEquals("打到的路径必须是 z-camuda 那条 @PostMapping",
                    "/api/approval-center/processes/start", stub.onlyRequest().path);
        }
    }

    /**
     * 请求头要真的上线。这一条是给 {@code CtcAdapter.doRequest} 的那只 NPE 准备的猎物：
     * {@code HttpRequestDefinition} 是裸 POJO，{@code getHttpRequestHeader()} 在没 set 过时返回 null，
     * 而旧写法直接 {@code def.getHttpRequestHeader().put(...)} —— doPostJson 恒带 Content-Type，
     * 于是 z-lc 每一次出站调用都在建请求这一步就炸，桩服务连一个字节都收不到。
     */
    @Test
    public void headersReachTheWire() throws Exception {
        try (CamudaStubServer stub = CamudaStubServer.ok()) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-h", "alice", "t", null);
            assertTrue("带头调用不该在建请求时炸掉: " + start.getFailure(), start.isStarted());
            CamudaStubServer.Recorded req = stub.onlyRequest();
            assertNotNull("请求头要收得到（收不到就是 doRequest 那一步就没建起来）", req.headers);
            assertEquals("POST", req.method);
            assertTrue("Content-Type 得是 JSON: " + req.header("content-type"),
                    req.header("content-type") != null && req.header("content-type").contains("application/json"));
        }
    }

    /** body 的键名必须是 {@code StartProcessRequestDTO} 真读的那几个（旧写法发 processDefKey ⇒ 引擎拿到 null）. */
    @Test
    public void postsTheBodyKeysZwfDtoReads() throws Exception {
        try (CamudaStubServer stub = CamudaStubServer.ok()) {
            inject(adapter, "baseUrl", stub.baseUrl());
            Map<String, Object> variables = new LinkedHashMap<String, Object>();
            variables.put("amount", 10);
            variables.put("approver", "carol");

            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "crm:order:7", "alice",
                    "order#7", variables);

            assertTrue(start.getInstanceId(), start.isStarted());
            String body = stub.onlyRequest().body;
            assertTrue("要用 DTO 的键名 processKey: " + body, body.contains("\"processKey\":\"expense\""));
            assertTrue("businessKey 要带上: " + body, body.contains("\"businessKey\":\"crm:order:7\""));
            assertTrue("initiator 要给（否则 z-camuda 把审批人兜底成常量 \"1\"）: " + body,
                    body.contains("\"initiator\":\"alice\""));
            assertTrue("title 要给: " + body, body.contains("\"title\":\"order#7\""));
            assertTrue("变量整份带走: " + body, body.contains("\"amount\":10") && body.contains("\"approver\":\"carol\""));
            assertFalse("旧键名 processDefKey 是 DTO 不读的: " + body, body.contains("processDefKey"));
        }
    }

    /** 给不出发起人时不要塞一个假用户进去——留给 z-camuda 用它自己的兜底，账目才对得上. */
    @Test
    public void omitsBlankInitiatorAndTitle() throws Exception {
        try (CamudaStubServer stub = CamudaStubServer.ok()) {
            inject(adapter, "baseUrl", stub.baseUrl());
            adapter.startProcess("expense", "biz-2", "   ", null, null);

            String body = stub.onlyRequest().body;
            assertFalse("空白 initiator 不该进 body: " + body, body.contains("\"initiator\""));
            assertFalse("null title 不该进 body: " + body, body.contains("\"title\""));
            assertTrue("processKey 仍在: " + body, body.contains("\"processKey\":\"expense\""));
        }
    }

    /** 实例 id 取 data.processInstanceId，不是整个 data Map 的 toString（旧写法回 {@code {processInstanceId=…}}）. */
    @Test
    public void readsInstanceIdFromDataProcessInstanceId() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .body("{\"success\":true,\"code\":200,\"data\":{\"processInstanceId\":\"wf-42\","
                        + "\"businessKey\":\"crm:order:7\",\"message\":\"流程启动成功\"}}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "crm:order:7", "alice", null, null);

            assertTrue(start.getFailure(), start.isStarted());
            assertEquals("wf-42", start.getInstanceId());
        }
    }

    @Test
    public void engineRejectionBecomesAFailureWithItsMessage() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .body("{\"success\":false,\"code\":500,\"message\":\"流程启动失败: business key 已存在\"}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-3", null, null, null);

            assertFalse("引擎说失败就不能算发起成功", start.isStarted());
            assertTrue("要把引擎那句原话带回去: " + start.getFailure(),
                    start.getFailure().contains("business key 已存在"));
        }
    }

    @Test
    public void httpFailureCarriesStatusAndPath() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(404).body("{\"timestamp\":1,\"status\":404,\"error\":\"Not Found\"}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-4", null, null, null);

            assertFalse(start.getFailure(), start.isStarted());
            assertTrue("要说清是 http 几: " + start.getFailure(), start.getFailure().contains("404"));
            assertTrue("要带上打的是哪条路径: " + start.getFailure(),
                    start.getFailure().contains("/api/approval-center/processes/start"));
        }
    }

    /**
     * 状态码非 2xx 时，哪怕应答体长得就是一份成功信封也不算发起成功。
     * <p>
     * 这条断言的猎物不在 body 上而在 http 状态上：z-util-http 的
     * {@code HttpExecutionResult:47} 写明"success 始终为 true，5xx 不会让 isSuccess()=false"，
     * 所以只判 {@code isSuccess()} 的写法（本类 #61 修复前的样子）会把 500 当成"远端受理了"，
     * 然后因为读不到 data 而报出一个把引擎撇干净的假原因。
     */
    @Test
    public void non2xxIsRefusedEvenWhenTheBodyLooksLikeASuccessEnvelope() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"data\":{\"processInstanceId\":\"ghost-1\"}}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-4b", null, null, null);

            assertFalse("500 就是没受理，body 里写什么都不算: " + start.getInstanceId(), start.isStarted());
            assertTrue("要说清是 http 500: " + start.getFailure(), start.getFailure().contains("500"));
        }
    }

    @Test
    public void unparsableBodyIsAFailure() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script().body("<html>网关错误</html>"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-5", null, null, null);

            assertFalse(start.getFailure(), start.isStarted());
            assertTrue("200 但回来的不是 JSON 对象也要判失败: " + start.getFailure(),
                    start.getFailure().contains("JSON"));
        }
    }

    /**
     * 空 body 是上面那一支的**另一条**路径（不是"解析失败"而是"解析出个空对象"）：
     * {@code JsonUtil.parseObject("")} 实测返回空 {@code JsonObject} 而不抛，
     * 所以旧写法那句"回了空应答体"永远不响。这里钉住"仍然判失败、仍然给得出原因"。
     */
    @Test
    public void emptyBodyIsAFailureWithItsOwnReason() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script().body(""))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-5b", null, null, null);

            assertFalse(start.getFailure(), start.isStarted());
            assertNotNull("失败要有一句话可以说: " + start.getFailure(), start.getFailure());
            assertTrue("要指出缺的是哪一格: " + start.getFailure(), start.getFailure().contains("success"));
        }
    }

    /** "success:true 而 data 里没有实例 id" 是这一族最难看见的形状：200、没报错、什么也没留下. */
    @Test
    public void successWithoutInstanceIdIsNotSuccess() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .body("{\"success\":true,\"code\":200,\"data\":{}}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-6", null, null, null);

            assertFalse(start.getFailure(), start.isStarted());
            assertTrue("要点名缺哪个字段: " + start.getFailure(),
                    start.getFailure().contains("processInstanceId"));
        }

        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .body("{\"success\":true,\"code\":200,\"data\":\"42\"}"))) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-7", null, null, null);

            assertFalse("data 不是对象时不能当成实例 id: " + start.getFailure(), start.isStarted());
        }
    }

    /** 连不上引擎不能变成抛错——调用方是用户的业务写路径. */
    @Test
    public void unreachableEngineIsAFailureNotAnException() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script().closeWithoutResponse())) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart start = adapter.startProcess("expense", "biz-8", null, null, null);

            assertFalse(start.isStarted());
            assertNotNull("失败要有一句话可以说: " + start.getFailure(), start.getFailure());
        }
    }

    @Test
    public void blankProcessKeyFailsWithoutTouchingTheNetwork() throws Exception {
        try (CamudaStubServer stub = CamudaStubServer.ok()) {
            inject(adapter, "baseUrl", stub.baseUrl());
            CamudaAdapter.ProcessStart nullKey = adapter.startProcess(null, "biz-9", null, null, null);
            CamudaAdapter.ProcessStart blankKey = adapter.startProcess("   ", "biz-9", null, null, null);

            assertFalse(nullKey.isStarted());
            assertFalse(blankKey.isStarted());
            assertEquals("KEY 为空时一次网络调用都不该发出去", 0, stub.requestCount());
        }
    }
}
