package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.exception.JsonTypeException;
import com.zifang.util.json.model.JsonObject;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Workflow 适配器: 调用 z-wf 的流程引擎, 在数据写后触发审批流.
 * <p>
 * 契约取自 z-wf 的源码而不是记忆（缺陷 #61 修的就是这三处对不上）：
 * <ul>
 *   <li>{@code z-wf-web/.../api/ApprovalCenterController.java:57} 的
 *       {@code @RequestMapping("/api/approval-center")} + {@code :617} 的
 *       {@code @PostMapping("/processes/start")} ⇒ 真实路径是
 *       {@code /api/approval-center/processes/start}。早先这里写的是
 *       {@code /approval-center/process/start}：既没有 {@code /api} 前缀，
 *       {@code process} 还少了一个 {@code s} —— 那样即使有人调用也只会撞 404。</li>
 *   <li>{@code StartProcessRequestDTO} 的字段叫 {@code processKey}（不是 {@code processDefKey}），
 *       而 {@code :625}/{@code :632} 会读 {@code initiator} 与 {@code title}：
 *       {@code initiator} 缺席时 {@code :644} 把审批人兜底成常量 {@code "1"}，
 *       于是低代码里谁提的单在审批中心都记成同一个用户。所以这两个字段必须由调用方给。</li>
 *   <li>成功回的是 {@code Result<Map<String,String>>}，实例 id 在 {@code data.processInstanceId}
 *       （{@code :663}）。早先这里 {@code r.getData().toString()} 直接把整个 Map 的
 *       {@code {processInstanceId=…, businessKey=…, message=…}} 当 id 返回。</li>
 * </ul>
 * 失败原因要带出去：{@link #startProcess} 的返回值同时承载"实例 id"和"为什么没起来"，
 * 因为写后触发是一次用户看不见的调用 —— 只回 null 的话，绑定行到底没兑现只能去翻日志。
 */
@Component
public class WfAdapter implements Adapter {

    /** z-wf 的启动流程端点（含 context 前缀，见类注释第一条）。 */
    static final String START_PATH = "/api/approval-center/processes/start";

    /**
     * 一次发起的等待预算，配置键.<b>派发侧（调用方最多等多久）与传输层（这一次调用最多占住
     * 一个线程多久）共用这一个键</b> —— 分成两个键就会有一个数没人对齐，而两边不一致的后果
     * 正是下面 {@link #transport} 注释里写的那件事。
     */
    public static final String TIMEOUT_PROPERTY = "z-lc.workflow.dispatch-timeout-ms";

    /** 默认预算：够一次正常发起（本机回环上实测个位数 ms），又远小于共享客户端那个 60s。 */
    public static final long DEFAULT_TIMEOUT_MS = 3000L;

    /**
     * 传输层比调用方的耐心多留的收尾时间。留余量是为了<b>让"谁先判失败"是确定的</b>：
     * 两边掐在同一个数上，账上那行 FAILED 的原因就会在"派发超时"和"socket 超时"之间随机漂。
     */
    static final long SOCKET_GRACE_MS = 500L;

    public static final String NAME = "wf";
    private static final Logger log = LogManager.getLogger(WfAdapter.class);

    @Value("${z-lc.adapter.wf.base-url:http://localhost:8888}")
    private String baseUrl;

    private volatile long timeoutMs = DEFAULT_TIMEOUT_MS;

    private volatile HttpExecutor transport = newTransport(DEFAULT_TIMEOUT_MS + SOCKET_GRACE_MS);

    /**
     * 只给这一条链路用的短超时执行器，<b>不用</b>共享的 {@code HttpExecutor.getDefault()}。
     * <p>
     * 为什么（这是契约层实测出来的，不是设想）：派发侧 {@code future.get(timeout)} 只是把
     * <b>等的人</b>放走，那条 HTTP 调用还压在 {@code workers} 的槽位上直到它自己结束 ——
     * 而共享客户端的 readTimeout 是 60s。于是"连续两次挂死的发起"就把 2 个槽占满，之后
     * <b>每一条新记录的绑定都发不出去</b>（账上写着「并发发起已达上限」），实测见
     * {@code WorkflowTriggerContractTest#timeoutDoesNotPoisonTheNextFires}。
     * "等待必须有上限"这句要成立，上限必须落在持有 socket 的那一层，不只是落在调用方。
     * <p>
     * 顺带两件事：连接不重复使用（挂死的那条 socket 不该留在池里等下一次被借走）、
     * 不重试（预算之内重一次只是把用户的写入按得更久）。
     */
    private static HttpExecutor newTransport(long budgetMs) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(budgetMs, TimeUnit.MILLISECONDS)
                .readTimeout(budgetMs, TimeUnit.MILLISECONDS)
                .writeTimeout(budgetMs, TimeUnit.MILLISECONDS)
                // callTimeout 才是"这一次调用总共多久"的上限：readTimeout 是"两次数据之间"，
                // 对方每 10ms 挤一个字节就能把它一直续下去。
                .callTimeout(budgetMs, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                .retryOnConnectionFailure(false)
                .build();
        return new HttpExecutor(client);
    }

    @Value("${" + TIMEOUT_PROPERTY + ":" + DEFAULT_TIMEOUT_MS + "}")
    void setTimeoutMs(long timeoutMs) {
        long budget = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        this.timeoutMs = budget;
        this.transport = newTransport(budget + SOCKET_GRACE_MS);
        log.info("WfAdapter 发起预算={}ms（传输层多留 {}ms 收尾）", budget, SOCKET_GRACE_MS);
    }

    /** 当前传输层预算，给断言用（派发侧那个数见 {@code WorkflowTriggerDispatcher}）。 */
    long socketBudgetMs() {
        return timeoutMs + SOCKET_GRACE_MS;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 25;
    }

    @Override
    public void init() {
        log.info("WfAdapter initialized, baseUrl={}", baseUrl);
    }

    /** 给报错/日志用的 URL；单测靠它钉住"打的是 z-wf 真实存在的那条路径"。 */
    String startUrl() {
        return baseUrl + START_PATH;
    }

    /**
     * 启动一个流程实例。任何失败都不抛：调用方是业务写路径，外部引擎不可用不能把用户的写入带走。
     *
     * @param processKey  流程定义 KEY（z-wf 的 {@code StartProcessRequestDTO.processKey}）
     * @param businessKey 业务唯一标识
     * @param initiator   发起人；给不出时宁可传 null，让 z-wf 用它自己的兜底，也不在这里编一个用户
     * @param title       流程标题
     * @param variables   流程变量
     */
    public ProcessStart startProcess(String processKey, String businessKey, String initiator,
                                     String title, Map<String, Object> variables) {
        if (processKey == null || processKey.trim().isEmpty()) {
            return ProcessStart.failed("流程定义 KEY 为空，没有发起任何流程");
        }
        Map<String, String> headers = new HashMap<>(JwtAwareHttpSupport.currentAuthHeaders());
        Map<String, Object> body = new HashMap<>();
        body.put("processKey", processKey);
        body.put("businessKey", businessKey);
        if (initiator != null && !initiator.trim().isEmpty()) {
            body.put("initiator", initiator);
        }
        if (title != null && !title.trim().isEmpty()) {
            body.put("title", title);
        }
        if (variables != null) {
            body.put("variables", variables);
        }

        HttpExecutionResult res = CtcAdapter.doPostJson(transport, startUrl(), headers,
                JsonUtil.toJson(body));
        if (!CtcAdapter.httpAccepted(res)) {
            return ProcessStart.failed("POST " + START_PATH + " http=" + res.getStatus()
                    + " err=" + (res.getError() == null ? "unknown" : res.getError()));
        }
        // 信封只能逐格读，不能用 JsonUtil.fromJson(body, Result.class)：Result 的默认构造是 private，
        // 而这份 JSON 引擎建对象走 clazz.getDeclaredConstructor().newInstance()（没 setAccessible），
        // 实测 forJson(..., Result.class) 恒抛 IllegalAccessException、TypeReference<Result<...>> 恒抛
        // ClassCastException（ParameterizedTypeImpl 不能转 Class）。也就是说 z-wf 就算真的把流程起起来了，
        // 旧写法也只会回"应答不是可解析的 Result" —— 这是缺陷 #61 这条链上第四处对不上。
        JsonObject envelope;
        try {
            envelope = JsonUtil.parseObject(res.getBody());
        } catch (Exception ex) {
            return ProcessStart.failed("z-wf 的应答不是可解析的 JSON 对象: " + ex.getMessage());
        }
        Boolean success = envelope.getBoolean("success");
        if (success == null) {
            return ProcessStart.failed("z-wf 的应答里没有布尔型的 success 这一格，不能当成发起成功: "
                    + abbreviate(res.getBody()));
        }
        if (!success.booleanValue()) {
            return ProcessStart.failed("z-wf 拒绝发起: " + envelope.getString("message"));
        }
        if (!envelope.containsKey("data")) {
            return ProcessStart.failed("z-wf 说成功但 data 整格都不在");
        }
        JsonObject data;
        try {
            data = envelope.getJsonObject("data");
        } catch (JsonTypeException ex) {
            // data 是 null / 字符串之类时引擎抛这个，消息里得带上实际形状，否则排查只能猜。
            return ProcessStart.failed("z-wf 的 data 不是一个对象（实际值="
                    + abbreviate(String.valueOf(envelope.get("data"))) + "），拿不到 processInstanceId");
        }
        String instanceId = data.getString("processInstanceId");
        if (instanceId == null || instanceId.trim().isEmpty()) {
            return ProcessStart.failed("z-wf 的 data 里没有 processInstanceId（data 里有 "
                    + data.size() + " 格）");
        }
        return ProcessStart.started(instanceId.trim());
    }

    /** 报错消息里带原文可以，但不能把一整包 HTML/栈原样贴回去。 */
    private static String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() <= 200 ? value : value.substring(0, 200) + "…";
    }

    /** 一次发起的结果：要么带着 z-wf 的实例 id，要么带着为什么没有。 */
    public static final class ProcessStart {

        private final boolean started;
        private final String instanceId;
        private final String failure;

        private ProcessStart(boolean started, String instanceId, String failure) {
            this.started = started;
            this.instanceId = instanceId;
            this.failure = failure;
        }

        public static ProcessStart started(String instanceId) {
            return new ProcessStart(true, instanceId, null);
        }

        public static ProcessStart failed(String failure) {
            return new ProcessStart(false, null, failure);
        }

        public boolean isStarted() {
            return started;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getFailure() {
            return failure;
        }
    }
}
