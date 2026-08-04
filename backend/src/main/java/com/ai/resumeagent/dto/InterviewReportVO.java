package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面试报告
 */
@Data
@Builder
public class InterviewReportVO {

    private Long sessionId;

    private Integer totalScore;

    private Map<String, Integer> dimensionScores;

    private List<QuestionReview> questionReviews;

    private String summary;

    private List<String> suggestions;

    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class QuestionReview {
        private String question;
        private String comment;
    }
}
