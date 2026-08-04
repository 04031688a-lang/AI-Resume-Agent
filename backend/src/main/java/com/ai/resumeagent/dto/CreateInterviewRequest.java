package com.ai.resumeagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建面试请求
 */
@Data
public class CreateInterviewRequest {

    /** general/technical/behavioral */
    @NotBlank(message = "请选择面试类型")
    private String interviewType;

    /** 关联岗位（可选） */
    private Long jobId;

    private String title;
}
