package com.ai.resumeagent.security;

/**
 * 当前登录用户信息（放入 SecurityContext 的 principal）
 */
public record LoginUser(Long id, String username, Integer role) {
}
