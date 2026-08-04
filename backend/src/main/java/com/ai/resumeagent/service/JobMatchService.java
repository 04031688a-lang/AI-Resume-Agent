package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.JobMatchVO;

import java.util.List;

public interface JobMatchService {

    JobMatchVO match(Long jobId, Long userId);

    List<JobMatchVO> history(Long userId);
}
