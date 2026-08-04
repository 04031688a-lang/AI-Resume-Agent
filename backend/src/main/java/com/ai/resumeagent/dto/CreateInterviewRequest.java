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

    /** 目标企业（可选，不关联岗位时直接填写企业名，如：字节跳动） */
    private String targetCompany;

    private String title;
}
