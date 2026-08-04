package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.JobQuery;
import com.ai.resumeagent.dto.JobSaveRequest;
import com.ai.resumeagent.dto.JobVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.entity.Job;

public interface JobService {

    PageResult<JobVO> page(JobQuery query);

    JobVO detail(Long id);

    JobVO create(JobSaveRequest request);

    JobVO update(Long id, JobSaveRequest request);

    void delete(Long id);

    /** 供匹配服务使用 */
    Job getEntity(Long id);
}
