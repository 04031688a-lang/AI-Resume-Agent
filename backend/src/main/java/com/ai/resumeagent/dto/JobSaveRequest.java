package com.ai.resumeagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 新增/编辑岗位请求（管理员）
 */
@Data
public class JobSaveRequest {

    @NotBlank(message = "岗位名称不能为空")
    private String title;

    @NotBlank(message = "公司名称不能为空")
    private String company;

    private String industry;

    private String location;

    private Integer salaryMin;

    private Integer salaryMax;

    private String educationRequirement;

    private String experienceRequirement;

    private List<String> skills;

    private String jobDescription;
}
