package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.LoginRequest;
import com.ai.resumeagent.dto.LoginResponse;
import com.ai.resumeagent.dto.RegisterRequest;
import com.ai.resumeagent.dto.UserVO;
import com.ai.resumeagent.entity.User;
import com.ai.resumeagent.mapper.UserMapper;
import com.ai.resumeagent.security.JwtUtil;
import com.ai.resumeagent.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        if (StringUtils.hasText(request.getEmail())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail())) > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .user(toVO(user))
                .build();
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .school(user.getSchool())
                .major(user.getMajor())
                .education(user.getEducation())
                .graduationYear(user.getGraduationYear())
                .jobIntention(user.getJobIntention())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
