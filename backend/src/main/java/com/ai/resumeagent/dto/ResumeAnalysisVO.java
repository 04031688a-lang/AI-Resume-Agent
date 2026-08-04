package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历分析报告
 */
@Data
@Builder
public class ResumeAnalysisVO {

    private Long resumeId;

    /** 综合评分（0~100） */
    private Integer totalScore;

    /** 各维度评分 */
    private Map<String, Integer> dimensionScores;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    /** 1=成功，0=失败 */
    private Integer status;

    private LocalDateTime createdAt;
}
