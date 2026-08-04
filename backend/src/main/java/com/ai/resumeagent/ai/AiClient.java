package com.ai.resumeagent.ai;

/**
 * AI 大模型调用抽象接口
 * 当前实现为 DeepSeekAiClient，后续可扩展其他模型厂商
 */
public interface AiClient {

    /**
     * 普通对话
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 对话（可指定温度）
     */
    String chat(String systemPrompt, String userPrompt, double temperature);
}
