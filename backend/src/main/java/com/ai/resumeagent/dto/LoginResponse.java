package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    private String token;

    /** Token 有效期（秒） */
    private Long expiresIn;

    private UserVO user;
}
