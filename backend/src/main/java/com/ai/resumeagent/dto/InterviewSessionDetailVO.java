package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 面试会话详情（含消息）
 */
@Data
@Builder
public class InterviewSessionDetailVO {

    private InterviewSessionVO session;

    private List<InterviewMessageVO> messages;
}
