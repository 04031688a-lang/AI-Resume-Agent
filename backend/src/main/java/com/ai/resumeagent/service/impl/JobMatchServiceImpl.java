package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.ai.AiClient;
import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.JobMatchVO;
import com.ai.resumeagent.entity.Job;
import com.ai.resumeagent.entity.JobMatch;
import com.ai.resumeagent.entity.Resume;
import com.ai.resumeagent.mapper.JobMapper;
import com.ai.resumeagent.mapper.JobMatchMapper;
import com.ai.resumeagent.mapper.ResumeMapper;
import com.ai.resumeagent.service.JobMatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    private static final int MAX_RESUME_LENGTH = 8000;
    private static final String SYSTEM_PROMPT = """
            你是一名专业的招聘匹配专家。请根据求职者简历与目标岗位的要求，评估匹配程度。
            请严格只输出 JSON（不要包含 markdown 代码块或任何解释），格式如下：
            {
              "matchScore": 0,
              "matchReasons": ["匹配理由1", "匹配理由2"],
              "skillGaps": ["技能差距1", "技能差距2"]
            }
            要求：
            1. matchScore 为 0~100 的整数，综合考量技能、经验、学历、项目经历与岗位要求的匹配度；
            2. matchReasons 给出 2~4 条具体理由，指出简历中与岗位契合的点；
            3. skillGaps 列出求职者相对岗位要求缺失或不足的技能/经验，若没有差距则返回空数组；
            4. 使用简体中文回复。
            """;

    private final JobMatchMapper jobMatchMapper;
    private final JobMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public JobMatchVO match(Long jobId, Long userId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null || job.getStatus() == null || job.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在或已下架");
        }

        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getStatus, 2)
                .orderByDesc(Resume::getCreatedAt)
                .last("LIMIT 1"));
        if (resume == null || !StringUtils.hasText(resume.getParsedContent())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先上传并成功解析简历，再进行岗位匹配");
        }

        String resumeContent = resume.getParsedContent();
        if (resumeContent.length() > MAX_RESUME_LENGTH) {
            resumeContent = resumeContent.substring(0, MAX_RESUME_LENGTH);
        }

        String response = aiClient.chat(SYSTEM_PROMPT,
                "岗位信息：\n" + buildJobInfo(job) + "\n\n求职者简历：\n" + resumeContent, 0.4);
        MatchData data;
        try {
            data = parseResult(response);
        } catch (JsonProcessingException e) {
            log.error("岗位匹配结果解析失败, jobId={}", jobId, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "匹配结果解析失败，请重试");
        }

        JobMatch match = jobMatchMapper.selectOne(new LambdaQueryWrapper<JobMatch>()
                .eq(JobMatch::getUserId, userId)
                .eq(JobMatch::getJobId, jobId)
                .last("LIMIT 1"));
        if (match == null) {
            match = new JobMatch();
            match.setUserId(userId);
            match.setJobId(jobId);
        }
        match.setMatchScore(data.matchScore);
        match.setMatchReasons(writeJsonList(data.matchReasons));
        match.setSkillGaps(writeJsonList(data.skillGaps));
        if (match.getId() == null) {
            jobMatchMapper.insert(match);
        } else {
            jobMatchMapper.updateById(match);
        }
        return toVO(match, job);
    }

    @Override
    public List<JobMatchVO> history(Long userId) {
        List<JobMatch> matches = jobMatchMapper.selectList(new LambdaQueryWrapper<JobMatch>()
                .eq(JobMatch::getUserId, userId)
                .orderByDesc(JobMatch::getUpdatedAt));
        if (matches.isEmpty()) {
            return List.of();
        }

        List<Long> jobIds = matches.stream().map(JobMatch::getJobId).distinct().toList();
        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        return matches.stream().map(m -> toVO(m, jobMap.get(m.getJobId()))).toList();
    }

    private String buildJobInfo(Job job) {
        return String.format("""
                        岗位名称：%s
                        公司：%s
                        行业：%s
                        地点：%s
                        薪资：%s-%s K
                        学历要求：%s
                        经验要求：%s
                        技能要求：%s
                        岗位描述：%s""",
                nullToEmpty(job.getTitle()),
                nullToEmpty(job.getCompany()),
                nullToEmpty(job.getIndustry()),
                nullToEmpty(job.getLocation()),
                job.getSalaryMin() == null ? "不限" : job.getSalaryMin(),
                job.getSalaryMax() == null ? "不限" : job.getSalaryMax(),
                nullToEmpty(job.getEducationRequirement()),
                nullToEmpty(job.getExperienceRequirement()),
                nullToEmpty(job.getSkills()),
                nullToEmpty(job.getJobDescription()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private MatchData parseResult(String raw) throws JsonProcessingException {
        String json = raw == null ? "" : raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        JsonNode root = objectMapper.readTree(json);
        return new MatchData(
                root.path("matchScore").asInt(0),
                readStringList(root, "matchReasons"),
                readStringList(root, "skillGaps"));
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

    private String writeJsonList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private JobMatchVO toVO(JobMatch match, Job job) {
        return JobMatchVO.builder()
                .id(match.getId())
                .jobId(match.getJobId())
                .jobTitle(job == null ? null : job.getTitle())
                .company(job == null ? null : job.getCompany())
                .matchScore(match.getMatchScore())
                .matchReasons(readJsonList(match.getMatchReasons()))
                .skillGaps(readJsonList(match.getSkillGaps()))
                .createdAt(match.getUpdatedAt() == null ? match.getCreatedAt() : match.getUpdatedAt())
                .build();
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

    private record MatchData(int matchScore, List<String> matchReasons, List<String> skillGaps) {
    }
}
