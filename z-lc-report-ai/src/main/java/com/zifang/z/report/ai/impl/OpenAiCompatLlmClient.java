package com.zifang.z.report.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.ai.LlmClient;
import com.zifang.z.report.ai.LlmConfig;
import com.zifang.z.report.ai.LlmMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
    private final ObjectMapper mapper = new ObjectMapper();

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
        ObjectNode body = mapper.createObjectNode();
        body.put("model", config.getModel());
        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature());
        }
        ArrayNode msgArr = body.putArray("messages");
        for (LlmMessage m : messages) {
            ObjectNode node = msgArr.addObject();
            node.put("role", m.getRole());
            node.put("content", m.getContent());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());
        String resp = http.postForObject(
                config.getBaseUrl() + "/chat/completions",
                new HttpEntity<>(body.toString(), headers),
                String.class);
        return extractContent(resp);
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
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
