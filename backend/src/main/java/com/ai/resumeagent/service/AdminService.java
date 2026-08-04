package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.AdminStatsVO;
import com.ai.resumeagent.dto.AiConfigSaveRequest;
import com.ai.resumeagent.dto.AiConfigVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.dto.UserVO;

public interface AdminService {

    PageResult<UserVO> pageUsers(String keyword, long page, long size);

    void updateUserStatus(Long userId, Integer status);

    void deleteUser(Long userId, Long operatorId);

    AdminStatsVO stats();

    AiConfigVO getAiConfig();

    void saveAiConfig(AiConfigSaveRequest request);
}
