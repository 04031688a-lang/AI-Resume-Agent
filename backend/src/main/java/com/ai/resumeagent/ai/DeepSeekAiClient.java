package com.ai.resumeagent.ai;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.service.AIConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek API 实现（OpenAI 兼容协议）
 */
@Slf4j
@Service
public class DeepSeekAiClient implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AIConfigService aiConfigService;

    @Value("${deepseek.api-key:}")
    private String defaultApiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String defaultBaseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String defaultModel;

    public DeepSeekAiClient(RestClient.Builder restClientBuilder,
                            ObjectMapper objectMapper,
                            AIConfigService aiConfigService) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.aiConfigService = aiConfigService;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 0.7);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, double temperature) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "未配置 DeepSeek API Key，请在本地配置或环境变量中设置 DEEPSEEK_API_KEY");
        }

        Map<String, Object> request = Map.of(
                "model", resolveModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temperature
        );

        try {
            String response = restClient.post()
                    .uri(resolveBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 返回结果异常：" + root);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务调用失败：" + e.getMessage());
        }
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, double temperature,
                           Consumer<String> onDelta) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "未配置 DeepSeek API Key，请在本地配置或环境变量中设置 DEEPSEEK_API_KEY");
        }

        Map<String, Object> request = Map.of(
                "model", resolveModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temperature,
                "stream", true
        );

        try {
            restClient.post()
                    .uri(resolveBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(request))
                    .exchange((req, res) -> consumeSse(res, onDelta));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API 流式调用失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务调用失败：" + e.getMessage());
        }
    }

    private Void consumeSse(ClientHttpResponse response, Consumer<String> onDelta) throws IOException {
        if (response.getStatusCode().isError()) {
            String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务返回错误：" + errorBody);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(data);
                JsonNode choices = node.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String delta = choices.get(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        onDelta.accept(delta);
                    }
                }
            }
        }
        return null;
    }

    private String resolveApiKey() {
        String fromDb = aiConfigService.get("deepseek.api_key");
        return StringUtils.hasText(fromDb) ? fromDb : defaultApiKey;
    }

    private String resolveModel() {
        String fromDb = aiConfigService.get("deepseek.model");
        return StringUtils.hasText(fromDb) ? fromDb : defaultModel;
    }

    private String resolveBaseUrl() {
        String fromDb = aiConfigService.get("deepseek.base_url");
        return StringUtils.hasText(fromDb) ? fromDb : defaultBaseUrl;
    }
}
