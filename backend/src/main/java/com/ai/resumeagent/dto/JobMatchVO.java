package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位匹配结果
 */
@Data
@Builder
public class JobMatchVO {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private String company;

    /** 匹配度（0~100） */
    private Integer matchScore;

    private List<String> matchReasons;

    private List<String> skillGaps;

    private LocalDateTime createdAt;
}
