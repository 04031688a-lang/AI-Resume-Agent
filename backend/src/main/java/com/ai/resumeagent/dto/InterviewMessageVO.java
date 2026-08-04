package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试消息
 */
@Data
@Builder
public class InterviewMessageVO {

    private Long id;

    private String role;

    private String content;

    private Integer round;

    private LocalDateTime createdAt;
}
