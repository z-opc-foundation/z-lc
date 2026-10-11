package com.zifang.z.report.ai;

/**
 * LLM 会话消息 (OpenAI chat 格式的角色约定: system/user/assistant)。
 */
public class LlmMessage {

    private String role;
    private String content;

    public LlmMessage() {
    }

    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
