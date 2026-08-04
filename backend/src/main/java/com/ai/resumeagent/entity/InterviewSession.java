package com.ai.resumeagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话表
 */
@Data
@TableName("interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联岗位（可选） */
    private Long jobId;

    /** 目标企业（不关联岗位时可直接填写，如：字节跳动） */
    private String targetCompany;

    /** 类型：general/technical/behavioral */
    private String interviewType;

    private String title;

    /** 0=进行中，1=已完成，2=已中断 */
    private Integer status;

    private Integer currentRound;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
