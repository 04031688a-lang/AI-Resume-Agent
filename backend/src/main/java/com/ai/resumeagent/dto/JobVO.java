package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位信息
 */
@Data
@Builder
public class JobVO {

    private Long id;

    private String title;

    private String company;

    private String industry;

    private String location;

    private Integer salaryMin;

    private Integer salaryMax;

    private String educationRequirement;

    private String experienceRequirement;

    private List<String> skills;

    private String jobDescription;

    private Integer status;

    private LocalDateTime createdAt;
}
