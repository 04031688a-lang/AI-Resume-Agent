package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.JobQuery;
import com.ai.resumeagent.dto.JobSaveRequest;
import com.ai.resumeagent.dto.JobVO;
import com.ai.resumeagent.dto.PageResult;
import com.ai.resumeagent.entity.Job;
import com.ai.resumeagent.mapper.JobMapper;
import com.ai.resumeagent.service.JobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<JobVO> page(JobQuery query) {
        long page = query.getPage() == null || query.getPage() <= 0 ? 1 : query.getPage();
        long size = query.getSize() == null ? 10 : Math.min(Math.max(query.getSize(), 1), 50);

        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getStatus, 1);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Job::getTitle, keyword).or().like(Job::getCompany, keyword));
        }
        if (StringUtils.hasText(query.getLocation())) {
            wrapper.like(Job::getLocation, query.getLocation().trim());
        }
        if (StringUtils.hasText(query.getIndustry())) {
            wrapper.like(Job::getIndustry, query.getIndustry().trim());
        }
        if (query.getSalaryMin() != null) {
            wrapper.and(w -> w.isNull(Job::getSalaryMax).or().ge(Job::getSalaryMax, query.getSalaryMin()));
        }
        if (query.getSalaryMax() != null) {
            wrapper.and(w -> w.isNull(Job::getSalaryMin).or().le(Job::getSalaryMin, query.getSalaryMax()));
        }
        wrapper.orderByDesc(Job::getCreatedAt);

        Page<Job> result = jobMapper.selectPage(new Page<>(page, size), wrapper);
        List<JobVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    @Override
    public JobVO detail(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return toVO(job);
    }

    @Override
    public JobVO create(JobSaveRequest request) {
        Job job = new Job();
        applyRequest(job, request);
        job.setStatus(1);
        jobMapper.insert(job);
        return toVO(job);
    }

    @Override
    public JobVO update(Long id, JobSaveRequest request) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        applyRequest(job, request);
        jobMapper.updateById(job);
        return toVO(job);
    }

    @Override
    public void delete(Long id) {
        if (jobMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        jobMapper.deleteById(id);
    }

    @Override
    public Job getEntity(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null || job.getStatus() == null || job.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return job;
    }

    private void applyRequest(Job job, JobSaveRequest request) {
        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany());
        job.setIndustry(request.getIndustry());
        job.setLocation(request.getLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setEducationRequirement(request.getEducationRequirement());
        job.setExperienceRequirement(request.getExperienceRequirement());
        job.setSkills(writeJsonList(request.getSkills()));
        job.setJobDescription(request.getJobDescription());
    }

    private JobVO toVO(Job job) {
        return JobVO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .industry(job.getIndustry())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .educationRequirement(job.getEducationRequirement())
                .experienceRequirement(job.getExperienceRequirement())
                .skills(readJsonList(job.getSkills()))
                .jobDescription(job.getJobDescription())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private String writeJsonList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = new ArrayList<>();
            objectMapper.readTree(json).forEach(item -> {
                if (item.isTextual()) {
                    list.add(item.asText());
                }
            });
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }
}
