package com.ai.resumeagent.dto;

import lombok.Data;

/**
 * 岗位查询条件
 */
@Data
public class JobQuery {

    private Long page = 1L;

    private Long size = 10L;

    /** 关键词（岗位/公司） */
    private String keyword;

    private String location;

    private String industry;

    private Integer salaryMin;

    private Integer salaryMax;
}
