package com.ai.resumeagent.controller;

import com.ai.resumeagent.ai.AiClient;
import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.AnswerRequest;
import com.ai.resumeagent.dto.CreateInterviewRequest;
import com.ai.resumeagent.dto.InterviewReportVO;
import com.ai.resumeagent.dto.InterviewSessionDetailVO;
import com.ai.resumeagent.dto.InterviewSessionVO;
import com.ai.resumeagent.entity.InterviewSession;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.InterviewService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 模拟面试接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final AiClient aiClient;

    @Resource(name = "sseExecutor")
    private Executor sseExecutor;

    @PostMapping
    public Result<InterviewSessionVO> create(@Valid @RequestBody CreateInterviewRequest request) {
        return Result.success(interviewService.create(UserContext.getCurrentUser().id(), request));
    }

    @GetMapping
    public Result<List<InterviewSessionVO>> history() {
        return Result.success(interviewService.history(UserContext.getCurrentUser().id()));
    }

    @GetMapping("/{id}")
    public Result<InterviewSessionDetailVO> detail(@PathVariable Long id) {
        return Result.success(interviewService.detail(UserContext.getCurrentUser().id(), id));
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
        Long userId = UserContext.getCurrentUser().id();
        SseEmitter emitter = new SseEmitter(120_000L);
        sseExecutor.execute(() -> streamAnswer(id, userId, request.getContent(), emitter));
        return emitter;
    }

    @PostMapping("/{id}/finish")
    public Result<InterviewReportVO> finish(@PathVariable Long id) {
        return Result.success(interviewService.finish(UserContext.getCurrentUser().id(), id));
    }

    @GetMapping("/{id}/report")
    public Result<InterviewReportVO> report(@PathVariable Long id) {
        return Result.success(interviewService.getReport(UserContext.getCurrentUser().id(), id));
    }

    private void streamAnswer(Long sessionId, Long userId, String content, SseEmitter emitter) {
        try {
            InterviewSession session = interviewService.getOwnedActive(sessionId, userId);
            int round = session.getCurrentRound();
            interviewService.saveUserMessage(sessionId, userId, content);

            String prompt = interviewService.buildChatPrompt(session, round);
            StringBuilder full = new StringBuilder();
            aiClient.chatStream(interviewSystemPrompt(), prompt, 0.7, delta -> {
                full.append(delta);
                sendEvent(emitter, "delta", Map.of("content", delta));
            });

            boolean lastRound = round >= interviewService.getMaxRounds();
            interviewService.saveAssistantMessageAndAdvance(session, full.toString());
            sendEvent(emitter, "done", Map.of("lastRound", lastRound));
            emitter.complete();
        } catch (BusinessException e) {
            log.warn("流式面试异常: {}", e.getMessage());
            sendError(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("流式面试异常", e);
            sendError(emitter, "AI 服务异常，请稍后重试");
        }
    }

    private String interviewSystemPrompt() {
        return String.format("""
                你是一名专业的 AI 模拟面试官，正在进行一场模拟面试。
                请对求职者刚才的回答给出简短点评（1~2 句，指出优点与不足），然后提出下一个面试问题。
                输出格式（严格两段，不要多余内容）：
                点评：<点评内容>
                提问：<下一个问题>

                规则：
                1. 问题要贴合面试类型与岗位要求，难度循序渐进；
                2. 如果当前已经是最后一轮（第 %d 轮），则「提问」部分替换为：本轮面试已结束，你可以点击「完成面试」生成报告；
                3. 使用简体中文。
                """, interviewService.getMaxRounds());
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            throw new IllegalStateException("SSE 发送失败", e);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(new BusinessException(ResultCode.SERVER_ERROR, message));
        }
    }
}
