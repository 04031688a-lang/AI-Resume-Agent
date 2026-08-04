package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历列表项
 */
@Data
@Builder
public class ResumeVO {

    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    /** 0=待解析，1=解析中，2=已完成，3=失败 */
    private Integer status;

    private LocalDateTime createdAt;
}
