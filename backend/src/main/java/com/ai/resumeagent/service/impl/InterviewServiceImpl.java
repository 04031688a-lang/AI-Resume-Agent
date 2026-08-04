package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.ai.AiClient;
import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.CreateInterviewRequest;
import com.ai.resumeagent.dto.InterviewMessageVO;
import com.ai.resumeagent.dto.InterviewReportVO;
import com.ai.resumeagent.dto.InterviewSessionDetailVO;
import com.ai.resumeagent.dto.InterviewSessionVO;
import com.ai.resumeagent.entity.InterviewMessage;
import com.ai.resumeagent.entity.InterviewReport;
import com.ai.resumeagent.entity.InterviewSession;
import com.ai.resumeagent.entity.Job;
import com.ai.resumeagent.mapper.InterviewMessageMapper;
import com.ai.resumeagent.mapper.InterviewReportMapper;
import com.ai.resumeagent.mapper.InterviewSessionMapper;
import com.ai.resumeagent.mapper.JobMapper;
import com.ai.resumeagent.service.InterviewService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    public static final int MAX_ROUNDS = 6;

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "general", "通用面试",
            "technical", "技术面试",
            "behavioral", "行为面试");

    private static final String FIRST_QUESTION_SYSTEM = """
            你是一名专业的 AI 模拟面试官。请根据面试类型与岗位要求，提出第一个面试问题。
            要求：只输出一个问题本身，不要任何点评或多余内容，使用简体中文。
            """;

    private static final String REPORT_SYSTEM = """
            你是一名资深面试官。请根据本次模拟面试的完整对话，生成一份面试报告。
            严格只输出 JSON（不要包含 markdown 代码块或任何解释），格式如下：
            {
              "totalScore": 0,
              "dimensionScores": {
                "expression": 0,
                "content": 0,
                "logic": 0,
                "profession": 0
              },
              "questionReviews": [
                {"question": "面试题目", "comment": "对该回答的点评"}
              ],
              "summary": "整体表现总结",
              "suggestions": ["改进建议1", "改进建议2"]
            }
            要求：
            1. totalScore 与各维度评分为 0~100 的整数；
            2. questionReviews 覆盖每道题目，点评具体；
            3. summary 概括整体表现，suggestions 给出可执行的改进建议；
            4. 使用简体中文。
            """;

    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewReportMapper reportMapper;
    private final JobMapper jobMapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewSessionVO create(Long userId, CreateInterviewRequest request) {
        String type = request.getInterviewType();
        if (!TYPE_LABELS.containsKey(type)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "面试类型不正确");
        }
        Job job = request.getJobId() == null ? null : jobMapper.selectById(request.getJobId());
        if (request.getJobId() != null && (job == null || job.getStatus() == null || job.getStatus() != 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "关联岗位不存在");
        }

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setJobId(job == null ? null : job.getId());
        session.setInterviewType(type);
        session.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle()
                : (job == null ? TYPE_LABELS.get(type) : TYPE_LABELS.get(type) + " · " + job.getTitle()));
        session.setStatus(0);
        session.setCurrentRound(1);
        sessionMapper.insert(session);

        String firstQuestion = aiClient.chat(FIRST_QUESTION_SYSTEM,
                buildContext(type, job, 1), 0.7);
        saveMessage(session.getId(), "assistant", firstQuestion, 1);
        return toSessionVO(session, job);
    }

    @Override
    public List<InterviewSessionVO> history(Long userId) {
        List<InterviewSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreatedAt));
        Map<Long, Job> jobMap = loadJobs(sessions);
        return sessions.stream().map(s -> toSessionVO(s, jobMap.get(s.getJobId()))).toList();
    }

    @Override
    public InterviewSessionDetailVO detail(Long userId, Long sessionId) {
        InterviewSession session = getOwned(sessionId, userId);
        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getId));
        Job job = session.getJobId() == null ? null : jobMapper.selectById(session.getJobId());
        return InterviewSessionDetailVO.builder()
                .session(toSessionVO(session, job))
                .messages(messages.stream().map(this::toMessageVO).toList())
                .build();
    }

    @Override
    public InterviewSession getOwnedActive(Long sessionId, Long userId) {
        InterviewSession session = getOwned(sessionId, userId);
        if (session.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该面试已结束");
        }
        return session;
    }

    @Override
    public void saveUserMessage(Long sessionId, Long userId, String content) {
        InterviewSession session = getOwnedActive(sessionId, userId);
        saveMessage(sessionId, "user", content, session.getCurrentRound());
    }

    @Override
    public String buildChatPrompt(InterviewSession session, int round) {
        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, session.getId())
                .orderByAsc(InterviewMessage::getId));
        StringBuilder history = new StringBuilder();
        for (InterviewMessage message : messages) {
            history.append("user".equals(message.getRole()) ? "求职者：" : "面试官：")
                    .append(message.getContent())
                    .append("\n\n");
        }
        Job job = session.getJobId() == null ? null : jobMapper.selectById(session.getJobId());
        return buildContext(TYPE_LABELS.getOrDefault(session.getInterviewType(), "通用面试"), job, round)
                + "\n当前轮次：第 " + round + " 轮 / 共 " + MAX_ROUNDS + " 轮\n\n历史对话：\n" + history;
    }

    @Override
    public void saveAssistantMessageAndAdvance(InterviewSession session, String content) {
        saveMessage(session.getId(), "assistant", content, session.getCurrentRound());
        if (session.getCurrentRound() < MAX_ROUNDS) {
            session.setCurrentRound(session.getCurrentRound() + 1);
            sessionMapper.updateById(session);
        }
    }

    @Override
    public InterviewReportVO finish(Long userId, Long sessionId) {
        InterviewSession session = getOwned(sessionId, userId);
        if (session.getStatus() == 0) {
            session.setStatus(1);
            sessionMapper.updateById(session);
        }

        List<InterviewMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getId));
        if (messages.size() < 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "对话内容过少，无法生成面试报告");
        }

        StringBuilder conversation = new StringBuilder();
        for (InterviewMessage message : messages) {
            conversation.append("user".equals(message.getRole()) ? "求职者：" : "面试官：")
                    .append(message.getContent())
                    .append("\n\n");
        }

        String response = aiClient.chat(REPORT_SYSTEM, conversation.toString(), 0.5);
        ReportData data;
        try {
            data = parseReport(response);
        } catch (JsonProcessingException e) {
            log.error("面试报告解析失败, sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "面试报告生成失败，请重试");
        }

        InterviewReport report = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (report == null) {
            report = new InterviewReport();
            report.setSessionId(sessionId);
        }
        report.setTotalScore(data.totalScore);
        report.setDimensionScores(writeJsonMap(data.dimensionScores));
        report.setQuestionReviews(writeJsonQuestionReviews(data.questionReviews));
        report.setSummary(data.summary);
        report.setSuggestions(writeJsonList(data.suggestions));
        if (report.getId() == null) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }
        return toReportVO(report);
    }

    @Override
    public InterviewReportVO getReport(Long userId, Long sessionId) {
        getOwned(sessionId, userId);
        InterviewReport report = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该面试尚未生成报告");
        }
        return toReportVO(report);
    }

    @Override
    public int getMaxRounds() {
        return MAX_ROUNDS;
    }

    private InterviewSession getOwned(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "面试会话不存在");
        }
        return session;
    }

    private void saveMessage(Long sessionId, String role, String content, Integer round) {
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setRound(round);
        messageMapper.insert(message);
    }

    private String buildContext(String type, Job job, Integer round) {
        StringBuilder builder = new StringBuilder("面试类型：").append(type).append("\n");
        if (job != null) {
            builder.append("关联岗位：").append(job.getTitle()).append(" @ ").append(job.getCompany()).append("\n");
            builder.append("岗位技能要求：").append(job.getSkills() == null ? "无" : job.getSkills()).append("\n");
            builder.append("岗位描述：").append(job.getJobDescription() == null ? "无" : job.getJobDescription()).append("\n");
        }
        return builder.toString();
    }

    private Map<Long, Job> loadJobs(List<InterviewSession> sessions) {
        List<Long> jobIds = sessions.stream()
                .map(InterviewSession::getJobId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        return jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
    }

    private InterviewSessionVO toSessionVO(InterviewSession session, Job job) {
        return InterviewSessionVO.builder()
                .id(session.getId())
                .jobId(session.getJobId())
                .jobTitle(job == null ? null : job.getTitle())
                .interviewType(session.getInterviewType())
                .title(session.getTitle())
                .status(session.getStatus())
                .currentRound(session.getCurrentRound())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private InterviewMessageVO toMessageVO(InterviewMessage message) {
        return InterviewMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .round(message.getRound())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ReportData parseReport(String raw) throws JsonProcessingException {
        String json = raw == null ? "" : raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        JsonNode root = objectMapper.readTree(json);

        Map<String, Integer> dimensions = new LinkedHashMap<>();
        JsonNode dims = root.path("dimensionScores");
        if (dims.isObject()) {
            dims.fields().forEachRemaining(entry -> dimensions.put(entry.getKey(), entry.getValue().asInt(0)));
        }

        List<InterviewReportVO.QuestionReview> reviews = new ArrayList<>();
        JsonNode reviewsNode = root.path("questionReviews");
        if (reviewsNode.isArray()) {
            reviewsNode.forEach(item -> reviews.add(InterviewReportVO.QuestionReview.builder()
                    .question(item.path("question").asText(""))
                    .comment(item.path("comment").asText(""))
                    .build()));
        }
        return new ReportData(
                root.path("totalScore").asInt(0),
                dimensions,
                reviews,
                root.path("summary").asText(""),
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

    private InterviewReportVO toReportVO(InterviewReport report) {
        return InterviewReportVO.builder()
                .sessionId(report.getSessionId())
                .totalScore(report.getTotalScore())
                .dimensionScores(readJsonMap(report.getDimensionScores()))
                .questionReviews(readJsonReviews(report.getQuestionReviews()))
                .summary(report.getSummary())
                .suggestions(readJsonList(report.getSuggestions()))
                .createdAt(report.getCreatedAt())
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

    private List<InterviewReportVO.QuestionReview> readJsonReviews(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<InterviewReportVO.QuestionReview> list = new ArrayList<>();
            objectMapper.readTree(json).forEach(item ->
                    list.add(InterviewReportVO.QuestionReview.builder()
                            .question(item.path("question").asText(""))
                            .comment(item.path("comment").asText(""))
                            .build()));
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJsonMap(Map<String, Integer> map) {
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

    private String writeJsonQuestionReviews(List<InterviewReportVO.QuestionReview> reviews) {
        try {
            return objectMapper.writeValueAsString(reviews);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record ReportData(int totalScore,
                              Map<String, Integer> dimensionScores,
                              List<InterviewReportVO.QuestionReview> questionReviews,
                              String summary,
                              List<String> suggestions) {
    }
}
