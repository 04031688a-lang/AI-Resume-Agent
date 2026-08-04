package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 配置信息（敏感值脱敏）
 */
@Data
@Builder
public class AiConfigVO {

    private boolean apiKeyConfigured;

    /** 脱敏后的 API Key，如 sk-****1234 */
    private String apiKeyMasked;

    private String model;

    private String baseUrl;
}
