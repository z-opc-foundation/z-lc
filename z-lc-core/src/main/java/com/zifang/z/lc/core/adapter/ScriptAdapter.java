package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Script 适配器: 调用 z-script 表达式执行 API.
 * <p>
 * 设计哲学:
 * HTTP 走 z-opc 自己的 z-util-http (Library-First), 通过 {@link CtcAdapter#doPostJson(String, Map, String)} 共享
 * POST JSON 工具方法. 表达式每次都不同, 不做缓存.
 */
@Component
public class ScriptAdapter implements Adapter {

    public static final String NAME = "script";
    private static final Logger log = LogManager.getLogger(ScriptAdapter.class);

    @Value("${z-lc.adapter.script.base-url:http://localhost:8888}")
    private String baseUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public void init() {
        log.info("ScriptAdapter initialized, baseUrl={} (via z-util-http)", baseUrl);
    }

    /**
     * 求值表达式; 返回 Object (执行结果). 失败抛 RuntimeException.
     */
    public Object eval(String scriptCode, Map<String, Object> context) {
        if (scriptCode == null || scriptCode.isEmpty()) {
            throw new IllegalArgumentException("scriptCode must not be empty");
        }
        String url = baseUrl + "/api/script/" + scriptCode + "/run";
        Map<String, Object> body = context == null ? Collections.emptyMap() : context;
        String jsonBody = JsonUtil.toJson(body);
        Map<String, String> headers = new HashMap<>(JwtAwareHttpSupport.currentAuthHeaders());

        log.debug("ScriptAdapter → POST {} code={}", url, scriptCode);
        HttpExecutionResult res = CtcAdapter.doPostJson(url, headers, jsonBody);
        if (!res.isSuccess()) {
            log.warn("ScriptAdapter.eval failed: scriptCode={}, status={} err={}",
                    scriptCode, res.getStatus(), res.getError());
            throw new RuntimeException("Script eval failed: status=" + res.getStatus()
                    + " err=" + res.getError());
        }
        try {
            Map<?, ?> resp = JsonUtil.fromJson(res.getBody(), Map.class);
            if (resp == null) return null;
            return resp.get("data");
        } catch (Exception ex) {
            log.warn("ScriptAdapter.eval parse failed: scriptCode={}, msg={}", scriptCode, ex.getMessage());
            throw new RuntimeException("Script eval response parse failed: " + ex.getMessage(), ex);
        }
    }
}
