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
 * 典型用法:
 * - 低代码平台定义 "报销单" 表 → 用户提交后通过本 Adapter 触发 "报销审批" 流程
 * - 创建用户后触发 "账号开通审批"
 */
@Component
public class WfAdapter implements Adapter {

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

    /**
     * 启动一个流程实例.
     *
     * @param processDefKey 流程定义 KEY (例如 "expense-approval")
     * @param businessKey   业务主键 (例如 "expense_123")
     * @param variables     流程变量 (申请人/金额等)
     * @return 流程实例 ID; 失败时返回 null
     */
    public String startProcess(String processDefKey, String businessKey, Map<String, Object> variables) {
        if (processDefKey == null || processDefKey.isEmpty()) {
            log.warn("WfAdapter.startProcess: processDefKey is null");
            return null;
        }
        String url = baseUrl + "/approval-center/process/start";
        Map<String, String> headers = new HashMap<>(JwtAwareHttpSupport.currentAuthHeaders());
        headers.put("Content-Type", "application/json; charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("processDefKey", processDefKey);
        body.put("businessKey", businessKey);
        if (variables != null) {
            body.put("variables", variables);
        }
        String jsonBody = JsonUtil.toJson(body);
        log.debug("WfAdapter → POST {} (process={}, business={})", url, processDefKey, businessKey);
        HttpExecutionResult res = CtcAdapter.doPostJson(url, headers, jsonBody);
        if (!res.isSuccess()) {
            log.warn("WfAdapter.startProcess failed: status={} err={}",
                    res.getStatus(), res.getError());
            return null;
        }
        try {
            Result<?> r = JsonUtil.fromJson(res.getBody(), Result.class);
            if (r == null || r.getData() == null) {
                return null;
            }
            return r.getData().toString();
        } catch (Exception ex) {
            log.warn("WfAdapter.startProcess parse error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 挂起一个流程实例.
     */
    public boolean suspendProcess(String instanceId) {
        if (instanceId == null) {
            return false;
        }
        String url = baseUrl + "/approval-center/process/" + instanceId + "/suspend";
        Map<String, String> headers = new HashMap<>(JwtAwareHttpSupport.currentAuthHeaders());
        headers.put("Content-Type", "application/json; charset=UTF-8");
        HttpExecutionResult res = CtcAdapter.doRequest("POST", url, headers, "{}");
        return res.isSuccess();
    }

    /**
     * 健康检查.
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = CtcAdapter.doGet(
                    baseUrl + "/approval-center/health",
                    JwtAwareHttpSupport.currentAuthHeaders());
            return res.isSuccess();
        } catch (Exception ex) {
            return false;
        }
    }
}
