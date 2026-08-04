package com.ai.resumeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目经历优化请求
 */
@Data
public class OptimizeProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    private String role;

    @NotBlank(message = "项目描述不能为空")
    @Size(max = 5000, message = "项目描述最多 5000 字")
    private String originalContent;
}
