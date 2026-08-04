package com.ai.resumeagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位表
 */
@Data
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String company;

    private String industry;

    private String location;

    /** 薪资下限（K） */
    private Integer salaryMin;

    /** 薪资上限（K） */
    private Integer salaryMax;

    private String educationRequirement;

    private String experienceRequirement;

    /** 技能要求（JSON 数组） */
    private String skills;

    private String jobDescription;

    /** 0=下架，1=上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
