package com.zifang.z.lc.report.ai.impl;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.lc.report.ai.LlmClient;
import com.zifang.z.lc.report.ai.LlmConfig;
import com.zifang.z.lc.report.ai.LlmMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议实现 (自研轻量, 方案决策②)。
 * <p>
 * 请求: POST {baseUrl}/chat/completions {model, messages, temperature}
 * 响应: choices[0].message.content
 * <p>
 * 安全约定: apiKey 由部署方注入; 消息内容由上层保证不含敏感数据;
 * M3 的结构化产物解析 (schema JSON) 须做白名单校验, 禁止动态执行。
 */
public class OpenAiCompatLlmClient implements LlmClient {

    private final LlmConfig config;
    private final RestTemplate http;

    public OpenAiCompatLlmClient(LlmConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.http = new RestTemplate(factory);
    }

    @Override
    public String chat(List<LlmMessage> messages) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalStateException("llm.api-key not configured");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", config.getModel());
        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature());
        }
        List<Map<String, Object>> msgArr = new ArrayList<Map<String, Object>>();
        for (LlmMessage m : messages) {
            Map<String, Object> node = new LinkedHashMap<String, Object>();
            node.put("role", m.getRole());
            node.put("content", m.getContent());
            msgArr.add(node);
        }
        body.put("messages", msgArr);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());
        String resp = http.postForObject(
                config.getBaseUrl() + "/chat/completions",
                new HttpEntity<>(JsonUtil.toJson(body), headers),
                String.class);
        return extractContent(resp);
    }

    private String extractContent(String responseBody) {
        try {
            JsonCursor root = JsonCursor.of(JsonUtil.parse(responseBody));
            JsonCursor content = root.path("choices").at(0).path("message").path("content");
            if (content.isMissing()) {
                throw new IllegalStateException("unexpected llm response shape: " + responseBody);
            }
            return content.asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse llm response", e);
        }
    }
}
