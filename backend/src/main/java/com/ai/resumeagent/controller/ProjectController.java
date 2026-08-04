package com.ai.resumeagent.controller;

import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.dto.OptimizeProjectRequest;
import com.ai.resumeagent.dto.ProjectOptimizationVO;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.ProjectOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目经历优化接口
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectOptimizationService projectOptimizationService;

    @PostMapping("/optimize")
    public Result<ProjectOptimizationVO> optimize(@Valid @RequestBody OptimizeProjectRequest request) {
        return Result.success(projectOptimizationService.optimize(UserContext.getCurrentUser().id(), request));
    }

    @GetMapping
    public Result<List<ProjectOptimizationVO>> history() {
        return Result.success(projectOptimizationService.history(UserContext.getCurrentUser().id()));
    }

    @GetMapping("/{id}")
    public Result<ProjectOptimizationVO> detail(@PathVariable Long id) {
        return Result.success(projectOptimizationService.detail(UserContext.getCurrentUser().id(), id));
    }
}
