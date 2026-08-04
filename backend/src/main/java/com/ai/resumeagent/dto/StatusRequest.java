package com.ai.resumeagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 状态更新请求（0/1）
 */
@Data
public class StatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
