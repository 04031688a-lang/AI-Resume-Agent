package com.ai.resumeagent.controller;

import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.dto.AdminStatsVO;
import com.ai.resumeagent.dto.AiConfigSaveRequest;
import com.ai.resumeagent.dto.AiConfigVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.dto.StatusRequest;
import com.ai.resumeagent.dto.UserVO;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台接口（仅管理员）
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<PageResult<UserVO>> pageUsers(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.pageUsers(keyword, page, size));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id, UserContext.getCurrentUser().id());
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<AdminStatsVO> stats() {
        return Result.success(adminService.stats());
    }

    @GetMapping("/ai-config")
    public Result<AiConfigVO> getAiConfig() {
        return Result.success(adminService.getAiConfig());
    }

    @PutMapping("/ai-config")
    public Result<Void> saveAiConfig(@RequestBody AiConfigSaveRequest request) {
        adminService.saveAiConfig(request);
        return Result.success();
    }
}
