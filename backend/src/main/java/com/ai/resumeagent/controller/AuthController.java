package com.ai.resumeagent.controller;

import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.dto.LoginRequest;
import com.ai.resumeagent.dto.LoginResponse;
import com.ai.resumeagent.dto.RegisterRequest;
import com.ai.resumeagent.dto.UserVO;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 个人信息
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile(UserContext.getCurrentUser().id()));
    }
}
