package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历详情（含解析文本预览）
 */
@Data
@Builder
public class ResumeDetailVO {

    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    /** 解析文本预览（前 2000 字） */
    private String contentPreview;

    private LocalDateTime createdAt;
}
