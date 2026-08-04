package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.LoginRequest;
import com.ai.resumeagent.dto.LoginResponse;
import com.ai.resumeagent.dto.RegisterRequest;
import com.ai.resumeagent.dto.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 注册 / 登录集成测试（使用本地 MySQL，事务回滚不落库）
 */
@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void registerThenLogin() {
        String username = "it_user_" + System.currentTimeMillis();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword("123456");
        UserVO vo = userService.register(registerRequest);

        assertNotNull(vo.getId());
        assertEquals(username, vo.getUsername());
        assertEquals(0, vo.getRole());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("123456");
        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response.getToken());
        assertEquals(username, response.getUser().getUsername());
    }
}
