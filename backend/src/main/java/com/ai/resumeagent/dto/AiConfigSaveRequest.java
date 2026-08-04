package com.ai.resumeagent.dto;

import lombok.Data;

/**
 * AI 配置保存请求（仅更新非空字段）
 */
@Data
public class AiConfigSaveRequest {

    private String apiKey;

    private String model;

    private String baseUrl;
}
