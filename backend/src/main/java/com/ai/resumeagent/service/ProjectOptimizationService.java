package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.OptimizeProjectRequest;
import com.ai.resumeagent.dto.ProjectOptimizationVO;

import java.util.List;

public interface ProjectOptimizationService {

    ProjectOptimizationVO optimize(Long userId, OptimizeProjectRequest request);

    List<ProjectOptimizationVO> history(Long userId);

    ProjectOptimizationVO detail(Long userId, Long id);
}
