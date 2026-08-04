package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.AdminStatsVO;
import com.ai.resumeagent.dto.AiConfigSaveRequest;
import com.ai.resumeagent.dto.AiConfigVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.dto.UserVO;
import com.ai.resumeagent.entity.InterviewSession;
import com.ai.resumeagent.entity.JobMatch;
import com.ai.resumeagent.entity.ProjectOptimization;
import com.ai.resumeagent.entity.Resume;
import com.ai.resumeagent.entity.ResumeAnalysis;
import com.ai.resumeagent.entity.User;
import com.ai.resumeagent.mapper.InterviewSessionMapper;
import com.ai.resumeagent.mapper.JobMatchMapper;
import com.ai.resumeagent.mapper.ProjectOptimizationMapper;
import com.ai.resumeagent.mapper.ResumeAnalysisMapper;
import com.ai.resumeagent.mapper.ResumeMapper;
import com.ai.resumeagent.mapper.UserMapper;
import com.ai.resumeagent.service.AIConfigService;
import com.ai.resumeagent.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String KEY_API = "deepseek.api_key";
    private static final String KEY_MODEL = "deepseek.model";
    private static final String KEY_BASE_URL = "deepseek.base_url";

    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper resumeAnalysisMapper;
    private final JobMatchMapper jobMatchMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final ProjectOptimizationMapper projectOptimizationMapper;
    private final AIConfigService aiConfigService;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String defaultBaseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String defaultModel;

    @Override
    public PageResult<UserVO> pageUsers(String keyword, long page, long size) {
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 10 : Math.min(size, 50);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getEmail, kw)
                    .or().like(User::getSchool, kw));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(p, s), wrapper);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), p, s, records);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态不正确");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long userId, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能删除当前登录账号");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        userMapper.deleteById(userId);
    }

    @Override
    public AdminStatsVO stats() {
        return AdminStatsVO.builder()
                .userTotal(userMapper.selectCount(null))
                .userActive(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1)))
                .resumeTotal(resumeMapper.selectCount(null))
                .resumeAnalysisTotal(resumeAnalysisMapper.selectCount(null))
                .jobMatchTotal(jobMatchMapper.selectCount(null))
                .interviewTotal(interviewSessionMapper.selectCount(null))
                .projectTotal(projectOptimizationMapper.selectCount(null))
                .build();
    }

    @Override
    public AiConfigVO getAiConfig() {
        String apiKey = aiConfigService.get(KEY_API);
        String model = aiConfigService.get(KEY_MODEL);
        String baseUrl = aiConfigService.get(KEY_BASE_URL);
        return AiConfigVO.builder()
                .apiKeyConfigured(StringUtils.hasText(apiKey))
                .apiKeyMasked(mask(apiKey))
                .model(StringUtils.hasText(model) ? model : defaultModel)
                .baseUrl(StringUtils.hasText(baseUrl) ? baseUrl : defaultBaseUrl)
                .build();
    }

    @Override
    public void saveAiConfig(AiConfigSaveRequest request) {
        aiConfigService.set(KEY_API, request.getApiKey(), "DeepSeek API Key");
        aiConfigService.set(KEY_MODEL, request.getModel(), "DeepSeek 模型");
        aiConfigService.set(KEY_BASE_URL, request.getBaseUrl(), "DeepSeek 接口地址");
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 8) {
            return value.substring(0, Math.min(2, value.length())) + "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
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
