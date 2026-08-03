package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.LoginRequest;
import com.ai.resumeagent.dto.LoginResponse;
import com.ai.resumeagent.dto.RegisterRequest;
import com.ai.resumeagent.dto.UserVO;

public interface UserService {

    UserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserVO getProfile(Long userId);
}
