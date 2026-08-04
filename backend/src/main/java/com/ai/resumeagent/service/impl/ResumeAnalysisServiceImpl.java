package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.ai.AiClient;
import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.ResumeAnalysisVO;
import com.ai.resumeagent.entity.Resume;
import com.ai.resumeagent.entity.ResumeAnalysis;
import com.ai.resumeagent.mapper.ResumeAnalysisMapper;
import com.ai.resumeagent.service.ResumeAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final String SYSTEM_PROMPT = """
            你是一名资深 HR 和简历优化专家，擅长评估应届生与技术岗位求职者的简历。
            请对用户提供的简历内容进行分析，并严格只输出 JSON（不要包含 markdown 代码块或任何解释），格式如下：
            {
              "totalScore": 0,
              "dimensionScores": {
                "content": 0,
                "structure": 0,
                "keywords": 0,
                "quantification": 0
              },
              "strengths": ["优点1", "优点2"],
              "weaknesses": ["不足1", "不足2"],
              "suggestions": ["具体改进建议1", "具体改进建议2"]
            }
            要求：
            1. totalScore 为 0~100 的整数；
            2. 每个维度评分为 0~100 的整数；
            3. 每条建议必须具体、可执行，最好包含示例改写；
            4. 使用简体中文回复。
            """;

    private final ResumeAnalysisMapper analysisMapper;
    private final ResumeServiceImpl resumeService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ResumeAnalysisVO analyze(Long resumeId, Long userId) {
        Resume resume = resumeService.getOwned(resumeId, userId);
        String content = resume.getParsedContent();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "简历内容为空，请重新上传");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setResumeId(resumeId);
        try {
            String response = aiClient.chat(SYSTEM_PROMPT, "简历内容：\n" + content, 0.6);
            AnalysisData data = parseResult(response);

            analysis.setTotalScore(data.totalScore);
            analysis.setDimensionScores(objectMapper.writeValueAsString(data.dimensionScores));
            analysis.setStrengths(objectMapper.writeValueAsString(data.strengths));
            analysis.setWeaknesses(objectMapper.writeValueAsString(data.weaknesses));
            analysis.setSuggestions(objectMapper.writeValueAsString(data.suggestions));
            analysis.setRawAiResponse(response);
            analysis.setStatus(1);
        } catch (BusinessException e) {
            analysis.setStatus(0);
            analysis.setRawAiResponse(e.getMessage());
            analysisMapper.insert(analysis);
            throw e;
        } catch (Exception e) {
            log.error("简历分析失败, resumeId={}", resumeId, e);
            analysis.setStatus(0);
            analysis.setRawAiResponse(e.getMessage());
            analysisMapper.insert(analysis);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 分析结果解析失败，请重试");
        }
        analysisMapper.insert(analysis);
        return toVO(analysis);
    }

    @Override
    public ResumeAnalysisVO getAnalysis(Long resumeId, Long userId) {
        resumeService.getOwned(resumeId, userId);
        ResumeAnalysis analysis = analysisMapper.selectOne(new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
                .orderByDesc(ResumeAnalysis::getCreatedAt)
                .last("LIMIT 1"));
        if (analysis == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该简历尚未生成分析报告");
        }
        return toVO(analysis);
    }

    private AnalysisData parseResult(String raw) throws JsonProcessingException {
        String json = raw == null ? "" : raw.trim();
        // 兼容 AI 偶尔输出的 ```json 包裹
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        JsonNode root = objectMapper.readTree(json);

        int totalScore = root.path("totalScore").asInt(0);
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        JsonNode dimensions = root.path("dimensionScores");
        if (dimensions.isObject()) {
            dimensions.fields().forEachRemaining(entry ->
                    dimensionScores.put(entry.getKey(), entry.getValue().asInt(0)));
        }
        return new AnalysisData(totalScore, dimensionScores,
                readStringList(root, "strengths"),
                readStringList(root, "weaknesses"),
                readStringList(root, "suggestions"));
    }

    private List<String> readStringList(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        JsonNode node = root.path(field);
        if (node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual()) {
                    result.add(item.asText());
                }
            });
        }
        return result;
    }

    private ResumeAnalysisVO toVO(ResumeAnalysis analysis) {
        return ResumeAnalysisVO.builder()
                .resumeId(analysis.getResumeId())
                .totalScore(analysis.getTotalScore())
                .dimensionScores(readJsonMap(analysis.getDimensionScores()))
                .strengths(readJsonList(analysis.getStrengths()))
                .weaknesses(readJsonList(analysis.getWeaknesses()))
                .suggestions(readJsonList(analysis.getSuggestions()))
                .status(analysis.getStatus())
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    private Map<String, Integer> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, Integer> map = new LinkedHashMap<>();
            objectMapper.readTree(json).fields().forEachRemaining(entry ->
                    map.put(entry.getKey(), entry.getValue().asInt(0)));
            return map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> readJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = new ArrayList<>();
            objectMapper.readTree(json).forEach(item -> {
                if (item.isTextual()) {
                    list.add(item.asText());
                }
            });
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private record AnalysisData(int totalScore,
                                Map<String, Integer> dimensionScores,
                                List<String> strengths,
                                List<String> weaknesses,
                                List<String> suggestions) {
    }
}
