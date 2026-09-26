package com.zifang.z.lc.core.adapter;

import com.zifang.util.core.meta.Result;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

    public static final String NAME = "wf";
    private static final Logger log = LogManager.getLogger(WfAdapter.class);

    @Value("${z-lc.adapter.wf.base-url:http://localhost:8888}")
    private String baseUrl;

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

        HttpExecutionResult res = CtcAdapter.doPostJson(startUrl(), headers, JsonUtil.toJson(body));
        if (!res.isSuccess()) {
            return ProcessStart.failed("POST " + START_PATH + " http=" + res.getStatus()
                    + " err=" + (res.getError() == null ? "unknown" : res.getError()));
        }
        Result<?> r;
        try {
            r = JsonUtil.fromJson(res.getBody(), Result.class);
        } catch (Exception ex) {
            return ProcessStart.failed("z-wf 的应答不是可解析的 Result: " + ex.getMessage());
        }
        if (r == null) {
            return ProcessStart.failed("z-wf 回了空应答体");
        }
        if (!r.isSuccess()) {
            return ProcessStart.failed("z-wf 拒绝发起: " + r.getMessage());
        }
        Object data = r.getData();
        if (!(data instanceof Map)) {
            return ProcessStart.failed("z-wf 成功但没有 data.processInstanceId（data="
                    + (data == null ? "null" : data.getClass().getSimpleName()) + "）");
        }
        Object instanceId = ((Map<?, ?>) data).get("processInstanceId");
        if (instanceId == null || instanceId.toString().trim().isEmpty()) {
            return ProcessStart.failed("z-wf 的 data 里没有 processInstanceId");
        }
        return ProcessStart.started(instanceId.toString());
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
