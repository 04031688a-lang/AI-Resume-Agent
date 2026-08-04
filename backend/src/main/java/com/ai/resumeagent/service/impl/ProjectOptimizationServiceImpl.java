package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.ai.AiClient;
import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.OptimizeProjectRequest;
import com.ai.resumeagent.dto.ProjectOptimizationVO;
import com.ai.resumeagent.entity.ProjectOptimization;
import com.ai.resumeagent.mapper.ProjectOptimizationMapper;
import com.ai.resumeagent.service.ProjectOptimizationService;
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
public class ProjectOptimizationServiceImpl implements ProjectOptimizationService {

    private static final String SYSTEM_PROMPT = """
            你是一名资深简历优化专家，擅长将项目经历改写得亮眼、量化、贴合岗位要求。
            请按 STAR 法则优化用户提供的项目描述。
            严格只输出 JSON（不要包含 markdown 代码块或任何解释），格式如下：
            {
              "optimizedContent": "优化后的完整项目经历文案（150~250 字，尽量包含量化指标）",
              "star": {
                "situation": "背景",
                "task": "任务",
                "action": "行动",
                "result": "结果"
              },
              "suggestions": ["关键词建议1", "量化建议2"]
            }
            要求：
            1. 保留用户提供的真实信息，不得编造数据；缺少量化时用「如：」给出可补充的示例；
            2. 语言精炼，突出个人贡献与结果；
            3. 使用简体中文。
            """;

    private final ProjectOptimizationMapper mapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ProjectOptimizationVO optimize(Long userId, OptimizeProjectRequest request) {
        String userPrompt = """
                项目名称：%s
                担任角色：%s
                原始描述：
                %s
                """.formatted(request.getProjectName(),
                StringUtils.hasText(request.getRole()) ? request.getRole() : "未填写",
                request.getOriginalContent());

        String response = aiClient.chat(SYSTEM_PROMPT, userPrompt, 0.5);
        OptimizeData data;
        try {
            data = parseResult(response);
        } catch (JsonProcessingException e) {
            log.error("项目优化结果解析失败", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "优化结果解析失败，请重试");
        }

        ProjectOptimization record = new ProjectOptimization();
        record.setUserId(userId);
        record.setProjectName(request.getProjectName());
        record.setRole(request.getRole());
        record.setOriginalContent(request.getOriginalContent());
        record.setOptimizedContent(data.optimizedContent);
        record.setStarContent(writeJsonMap(data.star));
        record.setSuggestions(writeJsonList(data.suggestions));
        mapper.insert(record);
        return toVO(record);
    }

    @Override
    public List<ProjectOptimizationVO> history(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<ProjectOptimization>()
                        .eq(ProjectOptimization::getUserId, userId)
                        .orderByDesc(ProjectOptimization::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ProjectOptimizationVO detail(Long userId, Long id) {
        ProjectOptimization record = mapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优化记录不存在");
        }
        return toVO(record);
    }

    private OptimizeData parseResult(String raw) throws JsonProcessingException {
        String json = raw == null ? "" : raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        JsonNode root = objectMapper.readTree(json);

        Map<String, String> star = new LinkedHashMap<>();
        JsonNode starNode = root.path("star");
        if (starNode.isObject()) {
            starNode.fields().forEachRemaining(entry -> star.put(entry.getKey(), entry.getValue().asText("")));
        }
        return new OptimizeData(
                root.path("optimizedContent").asText(""),
                star,
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

    private ProjectOptimizationVO toVO(ProjectOptimization record) {
        return ProjectOptimizationVO.builder()
                .id(record.getId())
                .projectName(record.getProjectName())
                .role(record.getRole())
                .originalContent(record.getOriginalContent())
                .optimizedContent(record.getOptimizedContent())
                .starContent(readJsonMap(record.getStarContent()))
                .suggestions(readJsonList(record.getSuggestions()))
                .createdAt(record.getCreatedAt())
                .build();
    }

    private Map<String, String> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, String> map = new LinkedHashMap<>();
            objectMapper.readTree(json).fields().forEachRemaining(entry ->
                    map.put(entry.getKey(), entry.getValue().asText("")));
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

    private String writeJsonMap(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String writeJsonList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record OptimizeData(String optimizedContent,
                                Map<String, String> star,
                                List<String> suggestions) {
    }
}
