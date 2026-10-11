package com.zifang.z.report.ai;

/**
 * LLM 连接配置 (OpenAI 兼容: 配 base-url/api-key/model 即可接 Qwen/GLM/DeepSeek)。
 * 密钥来源走环境变量/配置中心, 不落代码库。
 */
public class LlmConfig {

    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4o-mini";
    private Double temperature = 0.2;
    private int timeoutMs = 60_000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
