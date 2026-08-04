package com.ai.resumeagent.ai;

import java.util.function.Consumer;

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

    /**
     * 流式对话：通过 onDelta 逐段回调输出内容
     */
    default void chatStream(String systemPrompt, String userPrompt, double temperature,
                            Consumer<String> onDelta) {
        throw new UnsupportedOperationException("当前模型实现不支持流式输出");
    }
}
