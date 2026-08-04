package com.ai.resumeagent.controller;

import com.ai.resumeagent.common.Result;
import com.ai.resumeagent.dto.ResumeAnalysisVO;
import com.ai.resumeagent.dto.ResumeDetailVO;
import com.ai.resumeagent.dto.ResumeVO;
import com.ai.resumeagent.security.UserContext;
import com.ai.resumeagent.service.ResumeAnalysisService;
import com.ai.resumeagent.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历接口：上传 / 列表 / 详情 / 删除 / AI 分析
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;

    @PostMapping
    public Result<ResumeVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(resumeService.upload(file, UserContext.getCurrentUser().id()));
    }

    @GetMapping
    public Result<List<ResumeVO>> list() {
        return Result.success(resumeService.list(UserContext.getCurrentUser().id()));
    }

    @GetMapping("/{id}")
    public Result<ResumeDetailVO> detail(@PathVariable Long id) {
        return Result.success(resumeService.detail(id, UserContext.getCurrentUser().id()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resumeService.delete(id, UserContext.getCurrentUser().id());
        return Result.success();
    }

    @PostMapping("/{id}/analyze")
    public Result<ResumeAnalysisVO> analyze(@PathVariable Long id) {
        return Result.success(resumeAnalysisService.analyze(id, UserContext.getCurrentUser().id()));
    }

    @GetMapping("/{id}/analysis")
    public Result<ResumeAnalysisVO> analysis(@PathVariable Long id) {
        return Result.success(resumeAnalysisService.getAnalysis(id, UserContext.getCurrentUser().id()));
    }
}
