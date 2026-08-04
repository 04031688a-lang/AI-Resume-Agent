package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.ResumeAnalysisVO;

public interface ResumeAnalysisService {

    ResumeAnalysisVO analyze(Long resumeId, Long userId);

    ResumeAnalysisVO getAnalysis(Long resumeId, Long userId);
}
