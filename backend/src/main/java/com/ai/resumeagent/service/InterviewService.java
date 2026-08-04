package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.CreateInterviewRequest;
import com.ai.resumeagent.dto.InterviewReportVO;
import com.ai.resumeagent.dto.InterviewSessionDetailVO;
import com.ai.resumeagent.dto.InterviewSessionVO;
import com.ai.resumeagent.entity.InterviewSession;

import java.util.List;

public interface InterviewService {

    InterviewSessionVO create(Long userId, CreateInterviewRequest request);

    List<InterviewSessionVO> history(Long userId);

    InterviewSessionDetailVO detail(Long userId, Long sessionId);

    InterviewSession getOwnedActive(Long sessionId, Long userId);

    void saveUserMessage(Long sessionId, Long userId, String content);

    String buildChatPrompt(InterviewSession session, int round);

    void saveAssistantMessageAndAdvance(InterviewSession session, String content);

    InterviewReportVO finish(Long userId, Long sessionId);

    InterviewReportVO getReport(Long userId, Long sessionId);

    int getMaxRounds();
}
