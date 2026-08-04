package com.ai.resumeagent.controller;

import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.dto.JobMatchVO;
import com.ai.resumeagent.dto.JobQuery;
import com.ai.resumeagent.dto.JobSaveRequest;
import com.ai.resumeagent.dto.JobVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.JobMatchService;
import com.ai.resumeagent.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 岗位接口：浏览 / 匹配 / 管理
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobMatchService jobMatchService;

    @GetMapping("/jobs")
    public Result<PageResult<JobVO>> page(JobQuery query) {
        return Result.success(jobService.page(query));
    }

    @GetMapping("/jobs/{id}")
    public Result<JobVO> detail(@PathVariable Long id) {
        return Result.success(jobService.detail(id));
    }

    @PostMapping("/jobs/{id}/match")
    public Result<JobMatchVO> match(@PathVariable Long id) {
        return Result.success(jobMatchService.match(id, UserContext.getCurrentUser().id()));
    }

    @GetMapping("/matches")
    public Result<List<JobMatchVO>> matches() {
        return Result.success(jobMatchService.history(UserContext.getCurrentUser().id()));
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<JobVO> create(@Valid @RequestBody JobSaveRequest request) {
        return Result.success(jobService.create(request));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<JobVO> update(@PathVariable Long id, @Valid @RequestBody JobSaveRequest request) {
        return Result.success(jobService.update(id, request));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.success();
    }
}
