package com.zifang.z.report.ai;

import java.util.List;

/**
 * LLM 客户端 SPI (方案决策②)。
 * <p>
 * z-report 全部 AI 能力 (NL2Report / 对话式微调 / schema 校验) 只依赖本接口;
 * 默认实现 OpenAiCompatLlmClient (自研轻量);
 * z-agent-llm-gateway 剥离发布后, 提供同接口 adapter 即可整体切换。
 */
public interface LlmClient {

    /** 单轮/多轮对话, 返回 assistant 回复文本 (结构化产物由调用方解析) */
    String chat(List<LlmMessage> messages);
}
