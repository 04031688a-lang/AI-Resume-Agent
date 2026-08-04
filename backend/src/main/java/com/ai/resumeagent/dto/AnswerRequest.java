package com.ai.resumeagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 面试回答请求
 */
@Data
public class AnswerRequest {

    @NotBlank(message = "回答内容不能为空")
    private String content;
}
