package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话信息
 */
@Data
@Builder
public class InterviewSessionVO {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private String interviewType;

    private String title;

    /** 0=进行中，1=已完成，2=已中断 */
    private Integer status;

    private Integer currentRound;

    private LocalDateTime createdAt;
}
