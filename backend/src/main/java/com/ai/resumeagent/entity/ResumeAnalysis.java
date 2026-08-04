package com.ai.resumeagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历分析报告表
 */
@Data
@TableName("resume_analysis")
public class ResumeAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    /** 综合评分（0~100） */
    private Integer totalScore;

    /** 各维度评分（JSON） */
    private String dimensionScores;

    /** 优点 */
    private String strengths;

    /** 不足 */
    private String weaknesses;

    /** 改进建议 */
    private String suggestions;

    /** AI 原始返回（排查用） */
    private String rawAiResponse;

    /** 1=成功，0=失败 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
