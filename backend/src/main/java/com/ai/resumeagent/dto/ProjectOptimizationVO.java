package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 项目优化结果
 */
@Data
@Builder
public class ProjectOptimizationVO {

    private Long id;

    private String projectName;

    private String role;

    private String originalContent;

    private String optimizedContent;

    /** STAR 四要素 */
    private Map<String, String> starContent;

    private List<String> suggestions;

    private LocalDateTime createdAt;
}
