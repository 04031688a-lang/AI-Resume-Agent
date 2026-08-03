package com.ai.resumeagent.security;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户
 */
public final class UserContext {

    private UserContext() {
    }

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
