package com.ai.resumeagent.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    SERVER_ERROR(500, "服务器内部错误"),
    AI_SERVICE_ERROR(502, "AI 服务调用失败");

    private final int code;
    private final String message;
}
