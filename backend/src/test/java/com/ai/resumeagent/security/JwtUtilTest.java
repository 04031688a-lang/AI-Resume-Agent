package com.ai.resumeagent.security;

import com.ai.resumeagent.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JWT 工具单元测试
 */
class JwtUtilTest {

    @Test
    void generateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 7200L);

        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(0);

        String token = jwtUtil.generateToken(user);
        assertNotNull(token);

        Claims claims = jwtUtil.parseToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("tester", claims.get("username", String.class));
        assertEquals(0, claims.get("role", Integer.class).intValue());
    }
}
